/*
 * Copyright 2022 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WaveProcess {
  double silenceRead = 0.24; // 5354 samples 0.334625 s, 210-240 ms
  double silenceWrite = 1.0; // 17299 samples 1.0811875 s

  public WaveProcess() {
    boolean assertsEnabled = false;
    assert assertsEnabled = true;
    if (!assertsEnabled) System.out.println("asserts not enabled");
  }

  /**
   * If there is a silence longer than silenceRead seconds, make sure it lasts silenceWrite seconds.
   * Fix the full stop pause that differs between tts engines.
   * @param filePath 16 bits mono wave file to be modified
   * @param silenceWrite as an argument so it can be adjusted before the speed transformation
   */
  public void fixSilence(Path filePath, double silenceWrite) {
    try {
      ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(filePath)).order(ByteOrder.LITTLE_ENDIAN);
      int riffMagic = buffer.getInt();
      assert riffMagic == 0x46464952;
      int riffSize = buffer.getInt();
      assert riffSize == buffer.capacity() - buffer.position();
      int waveMagic = buffer.getInt();
      assert waveMagic == 0x45564157;
      int fmtMagic = buffer.getInt();
      assert fmtMagic == 0x20746D66;
      int fmtSize = buffer.getInt();
      short formatTag = buffer.getShort();
      assert formatTag == 1;
      short channels = buffer.getShort();
      assert channels == 1; // any chance to have stereo voice?
      int samplesPerSec = buffer.getInt();
      int avgBytesPerSec = buffer.getInt();
      assert avgBytesPerSec == samplesPerSec * 2;
      short blockAlign = buffer.getShort();
      assert blockAlign == 2;
      buffer.position(buffer.position() + fmtSize - 14);
      int dataMagic = buffer.getInt();
      assert dataMagic == 0x61746164;
      int dataSize = buffer.getInt();
      assert dataSize == buffer.capacity() - buffer.position();
      assert dataSize % 2 == 0;
      int sampleSize = dataSize / 2;
      buffer.mark();

      int[] percentile = new int[sampleSize];
      for (int i = 0; i < sampleSize; i++) {
        percentile[i] = Math.abs(buffer.getShort());
      }
      Arrays.sort(percentile);
      int p50m = percentile[sampleSize / 2];
      buffer.reset();
      int st = p50m * 4 / 5;
      int sr = (int) Math.round(samplesPerSec * silenceRead);
      int sw = (int) Math.round(samplesPerSec * silenceWrite);

      int l = 0;
      int sizeIncrease = 0;
      List<Integer> list = new ArrayList<>();
      for (int i = 0; i < sampleSize; i++) {
        short p = buffer.getShort();
        if (Math.abs(p) < st) {
          l++;
        } else {
          if (l >= sr && l < sw && i != l) { // not saving head
            list.add(i);
            list.add(l);
            sizeIncrease += sw - l;
          }
          l = 0;
        }
      }
      // if (l >= sr) list.add(l); // not saving tail
      if (list.isEmpty()) return;

      // modify the file
      sizeIncrease *= 2;
      riffSize += sizeIncrease;
      buffer.putInt(0x04, riffSize);
      dataSize += sizeIncrease;
      buffer.putInt(0x18 + fmtSize, dataSize);
      byte[] wav = new byte[buffer.capacity() + sizeIncrease];
      int m0 = 0;
      int m1 = 0;
      for (int i = 1; i < list.size(); i += 2) {
        int ms = list.get(i);
        int mp = list.get(i - 1);
        int mr = (mp - ms / 2) * 2 + 0x1C + fmtSize;
        System.arraycopy(buffer.array(), m0, wav, m1, mr - m0);
        m1 += (sw - ms) * 2 + mr - m0;
        m0 = mr;
      }
      System.arraycopy(buffer.array(), m0, wav, m1, wav.length - m1);

      Files.write(filePath, wav);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public byte[] apply(TextRequest request) {
    if (request.fixSilence) {
      fixSilence(request.targetFile, request.speed.orElse(1) * silenceWrite);
    }

    Path processedFile = request.targetFile;
    if (request.speed.isPresent() || request.volume.isPresent() || request.mp3) {
      String fi = request.targetFile.toString();
      String fo = fi + (request.mp3 ? ".mp3" : ".wav");
      processedFile = Paths.get(fo);
      List<String> filter = new ArrayList<>();
      request.speed.ifPresent(speed -> filter.add(String.format("atempo=%f", speed)));
      request.volume.ifPresent(vol -> filter.add(String.format("volume=%f", vol)));
      List<String> cmd = new ArrayList<>();
      cmd.add("ffmpeg");
      cmd.add("-loglevel");
      cmd.add("error"); // loglevel must be error, warning produce this output:
                        // [libmp3lame @ 0x55e661f03f40] Trying to remove 576 samples, but the queue is empty
      cmd.add("-channel_layout");
      cmd.add("mono"); // die if not
      cmd.add("-i");
      cmd.add(fi);
      if (!filter.isEmpty()) {
        cmd.add("-af");
        cmd.add(String.join(",", filter));
      }
      cmd.add("-y"); // Overwrite output files without asking
      cmd.add(fo);
      System.out.println(Instant.now() + " ffmpeg " + String.join(" ", filter));
      Exec.exec(cmd.toArray(new String[0]), "");
    }
    try {
      byte[] bytes = Files.readAllBytes(processedFile);
      Files.delete(processedFile);
      Files.deleteIfExists(request.targetFile);
      return bytes;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
