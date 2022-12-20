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
import java.util.logging.Logger;

public class Controller extends NanoHTTPD {

  private static final Logger log = Logger.getLogger("ab.festival-api");
  public static final int API_PORT = 59125;
  public static final String API_URI = "/process";

  public Controller() {
    super(API_PORT);
    mimeTypes();
    log.info(Exec.exec(new String[]{"festival"}, "(print (voice.list))"));
  }

  @Override
  public Response serve(IHTTPSession session) {
    String uri = session.getUri();
    if (uri.equals(API_URI)) {
      Map<String, String> map = session.getParms();
      try {
        Path wav = Files.createTempFile("festival-api_", ".wav");
        Exec.exec(new String[]{"text2wave", "-o", wav.toString()}, map.get("INPUT_TEXT"));
        return newFixedLengthResponse(Response.Status.OK, "audio/x-wav",
            Files.newInputStream(wav), Files.size(wav));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return super.serve(session);
  }

  public static void main(String[] args) throws IOException {
    Controller app = new Controller();
    app.start();
    System.in.read();
    app.stop();
  }
}
