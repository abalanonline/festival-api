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
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class Controller extends NanoHTTPD implements AutoCloseable {

  public static final int API_PORT = 59125;
  public static final String API_URI = "/process";

  private final Consumer<TextRequest> festival;
  private final Consumer<TextRequest> svoxPico;
  private final WaveProcess waveProcess;
  private final Set<UUID> fixSilence;

  public Controller() {
    super(API_PORT);
    mimeTypes();
    festival = new Festival();
    svoxPico = new SvoxPico();
    waveProcess = new WaveProcess();
    fixSilence = new HashSet<>();
    fixSilence.add(UUID.fromString("5a91d3df-f49f-3d4c-8f84-ef5792f5fb34"));
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
        Consumer<TextRequest> tts = voice == null || voice.startsWith("nanotts:")
            || voice.equals("svox") || voice.equals("ttspico")
            ? this.svoxPico : this.festival;
        TextRequest request = new TextRequest(map.get("INPUT_TEXT"), Files.createTempFile("festival-api_", ".wav"))
            .locale(map.get("LOCALE")).voice(voice);
        UUID voiceUuid = UUID.nameUUIDFromBytes(Optional.ofNullable(voice).orElse("").getBytes());
        request.fixSilence = fixSilence.contains(voiceUuid);
        Optional.ofNullable(map.get("SPEED")).ifPresent(s -> request.speed(Double.parseDouble(s)));
        Optional.ofNullable(map.get("VOLUME")).ifPresent(s -> request.volume(Double.parseDouble(s)));
        tts.accept(request);
        byte[] response = waveProcess.apply(request);
        System.out.println(Instant.now() + " ok");
        return newFixedLengthResponse(Response.Status.OK, "audio/x-wav",
            new ByteArrayInputStream(response), response.length);
      } catch (IOException | UncheckedIOException | ResponseException | Exec.ExecException e) {
        System.err.println(Instant.now());
        e.printStackTrace();
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
      }
    }
    return super.serve(session);
  }

}
