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

public class Festival implements Consumer<TextRequest> {

  private String voiceList;
  public Festival() {
    voiceList = Exec.exec(new String[]{"festival"}, "(print (voice.list))").trim();
    System.out.println(voiceList);
  }

  @Override
  public void accept(TextRequest request) {
    List<String> cmd = new ArrayList<>();
    cmd.add("text2wave");
    cmd.add("-o");
    cmd.add(request.targetFile.toString());

    // volume is nearly identical to ffmpeg
    request.volume.ifPresent(volume -> {
      cmd.add("-scale");
      cmd.add(String.format("%f", volume));
    });
    request.volume = OptionalDouble.empty();

    List<String> eval = new ArrayList<>();
    request.voice.ifPresent(voice -> eval.add("(voice_" + voice + ")"));
    // not reliable - ignored or inaccurately applied, commented for reference
    // request.speed.ifPresent(speed -> eval.add(String.format("(Parameter.set 'Duration_Stretch %f)", 1 / speed)));
    if (eval.size() > 0) {
      cmd.add("-eval");
      cmd.add(eval.size() == 1 ? eval.get(0) : "(list " + String.join(" ", eval) + ")");
    }

    Exec.exec(cmd.toArray(new String[0]), request.inputText);
  }
}
