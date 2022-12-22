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

public class SvoxPico implements Function<TextRequest, byte[]> {

  public static final String LOCALE_LIST = "en-US en-GB de-DE es-ES fr-FR it-IT";

  public SvoxPico() {
    System.out.println(LOCALE_LIST);
  }

  @Override
  public byte[] apply(TextRequest request) {
    byte[] bytes;
    try {
      Path wav = Files.createTempFile("festival-api_", ".wav");
      String cmd = "pico2wave -w " + wav;
      if (request.locale != null) {
        cmd += " -l " + request.locale.replace('_', '-'); // BCP 47, RFC 5646 sensitive
      }
      String tags = "";
      if (request.speed != null) {
        tags += String.format("<speed level=\"%d\">", (int) Math.round(request.speed * 100));
      }
      if (request.volume != null) {
        tags += String.format("<volume level=\"%d\">", (int) Math.round(request.volume * 100));
      }
      Exec.exec(cmd.split("\\s+"), tags + request.inputText);
      bytes = Files.readAllBytes(wav);
      Files.delete(wav);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes;
  }
}
