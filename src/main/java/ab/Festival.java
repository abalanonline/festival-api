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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Festival implements Function<TextRequest, byte[]> {

  private String voiceList;
  public Festival() {
    voiceList = Exec.exec(new String[]{"festival"}, "(print (voice.list))").trim();
    System.out.println(voiceList);
  }

  @Override
  public byte[] apply(TextRequest request) {
    byte[] bytes;
    try {
      List<String> cmd = new ArrayList<>();
      cmd.add("text2wave");
      Path wav = Files.createTempFile("festival-api_", ".wav");
      cmd.add("-o");
      cmd.add(wav.toString());

      if (request.volume != null) {
        cmd.add("-scale");
        cmd.add(String.format("%f", request.volume));
      }

      List<String> eval = new ArrayList<>();
      if (request.voice != null) {
        eval.add("(voice_" + request.voice + ")");
      }
      if (request.speed != null) {
        eval.add(String.format("(Parameter.set 'Duration_Stretch %f)", 1 / request.speed));
      }
      if (eval.size() > 0) {
        cmd.add("-eval");
        cmd.add(eval.size() == 1 ? eval.get(0) : "(list " + String.join(" ", eval) + ")");
      }

      Exec.exec(cmd.toArray(new String[0]), request.inputText);
      bytes = Files.readAllBytes(wav);
      Files.delete(wav);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes;
  }
}
