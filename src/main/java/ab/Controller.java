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

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Controller extends NanoHTTPD implements AutoCloseable {

  public static final int API_PORT = 59125;
  public static final String API_URI = "/process";

  public Controller() {
    super(API_PORT);
    mimeTypes();
    System.out.print(Exec.exec(new String[]{"festival"}, "(print (voice.list))"));
    try {
      System.out.println("start");
      start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    stop();
    System.out.println("stop");
  }

  @Override
  public Response serve(IHTTPSession session) {
    String uri = session.getUri();
    if (uri.equals(API_URI)) {
      Map<String, String> map = session.getParms();
      try {
        Path wav = Files.createTempFile("festival-api_", ".wav");
        String eval = "";
        String voice = map.get("VOICE");
        if (voice != null) {
          eval += " (voice_" + voice + ")";
        }
        eval += " (Parameter.set 'Duration_Stretch 1)";
        Exec.exec(new String[]{"text2wave", "-o", wav.toString(), "-eval", "(list" + eval + ")"},
            map.get("INPUT_TEXT"));
        return newFixedLengthResponse(Response.Status.OK, "audio/x-wav",
            Files.newInputStream(wav), Files.size(wav));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return super.serve(session);
  }

}
