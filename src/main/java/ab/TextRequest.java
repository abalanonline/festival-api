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

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalDouble;

public class TextRequest {
  public String inputText;
  public Optional<String> voice = Optional.empty();
  public Optional<String> locale = Optional.empty();
  public OptionalDouble speed = OptionalDouble.empty();
  public OptionalDouble volume = OptionalDouble.empty();
  public Path targetFile;

  public TextRequest(String inputText) {
    this.inputText = inputText;
  }

  public TextRequest voice(String voice) {
    this.voice = Optional.ofNullable(voice);
    return this;
  }

  public TextRequest locale(String locale) {
    this.locale = Optional.ofNullable(locale);
    return this;
  }

  public TextRequest speed(double speed) {
    this.speed = OptionalDouble.of(speed);
    return this;
  }

  public TextRequest volume(double volume) {
    this.volume = OptionalDouble.of(volume);
    return this;
  }

  public TextRequest targetFile(Path targetFile) {
    this.targetFile = targetFile;
    return this;
  }
}
