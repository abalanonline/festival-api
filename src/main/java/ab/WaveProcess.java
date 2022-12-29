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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WaveProcess {
  public byte[] apply(TextRequest request) {
    Path processedFile = request.targetFile;
    if (true) { // FIXME: 2022-12-29 must be conditional
      String fi = request.targetFile.toString();
      String fo = fi + ".wav";
      processedFile = Paths.get(fo);
      List<String> filter = new ArrayList<>();
      request.speed.ifPresent(speed -> filter.add(String.format("atempo=%f", speed)));
      request.volume.ifPresent(vol -> filter.add(String.format("volume=%f", vol)));
      List<String> cmd = new ArrayList<>();
      cmd.add("ffmpeg");
      cmd.add("-loglevel");
      cmd.add("warning");
      cmd.add("-channel_layout");
      cmd.add("mono"); // die if not
      cmd.add("-i");
      cmd.add(fi);
      if (!filter.isEmpty()) {
        cmd.add("-af");
        cmd.add(String.join(",", filter));
      }
      cmd.add(fo);
      System.out.println(Instant.now() + " " + String.join(" ", cmd)); // FIXME: 2022-12-29 reduce details
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
