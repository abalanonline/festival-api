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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public class SvoxPico implements Consumer<TextRequest> {

  public static final String[] LOCALE_LIST = {"en-US", "en-GB", "de-DE", "es-ES", "fr-FR", "it-IT"};

  public SvoxPico() {
    System.out.println(String.join(" ", LOCALE_LIST));
  }

  @Override
  public void accept(TextRequest request) {
    List<String> cmd = new ArrayList<>();
    cmd.add("pico2wave");
    cmd.add("-w");
    cmd.add(request.targetFile.toString());
    request.locale.ifPresent(locale -> {
      if (locale.length() == 2) {
        for (String s : LOCALE_LIST) {
          if (s.startsWith(locale)) {
            locale = s;
            break;
          }
        }
      }
      cmd.add("-l");
      cmd.add(locale.replace('_', '-')); // BCP 47, RFC 5646 sensitive
    });
    StringBuilder tags = new StringBuilder();
    // speed is identical to ffmpeg with the exception of pauses, they are not affected
    request.speed.ifPresent(speed -> tags.append(String.format("<speed level=\"%d\">", (int) Math.round(speed * 100))));
    request.speed = OptionalDouble.empty();
    // VOLUME=1.0 set the default <volume level="50">
    // identical to ffmpeg with <1% difference
    request.volume.ifPresent(vol -> tags.append(String.format("<volume level=\"%d\">", (int) Math.round(vol * 50))));
    request.volume = OptionalDouble.empty();
    Exec.exec(cmd.toArray(new String[0]), tags.toString() + request.inputText);
  }
}
