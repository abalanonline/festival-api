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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Function;

public class Controller extends NanoHTTPD implements AutoCloseable {

  public static final int API_PORT = 59125;
  public static final String API_URI = "/process";

  Function<TextRequest, byte[]> festival;
  Function<TextRequest, byte[]> svoxPico;

  public Controller() {
    super(API_PORT);
    mimeTypes();
    festival = new Festival();
    svoxPico = festival;
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
        if (Method.POST.equals(session.getMethod())) {
          session.parseBody(map);
        }
        String voice = map.get("VOICE");
        Function<TextRequest, byte[]> tts = voice == null || voice.startsWith("nanotts:")
            || voice.equals("svox") || voice.equals("ttspico")
            ? this.svoxPico : this.festival;
        TextRequest request = new TextRequest(map.get("INPUT_TEXT")).voice(voice);
        byte[] response = tts.apply(request);
        return newFixedLengthResponse(Response.Status.OK, "audio/x-wav",
            new ByteArrayInputStream(response), response.length);
      } catch (IOException | UncheckedIOException | ResponseException | Exec.ExecException e) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
      }
    }
    return super.serve(session);
  }

}
