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
      Path wav = Files.createTempFile("festival-api_", ".wav");
      String eval = "";
      if (request.voice != null) {
        eval += " (voice_" + request.voice + ")";
      }
      eval += " (Parameter.set 'Duration_Stretch 1)";
      Exec.exec(new String[]{"text2wave", "-o", wav.toString(), "-eval", "(list" + eval + ")"},
          request.inputText);
      bytes = Files.readAllBytes(wav);
      Files.delete(wav);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes;
  }
}
