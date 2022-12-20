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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Exec {

  /**
   * Reads all bytes from an input stream and writes them to an output stream.
   */
  private static final int BUFFER_SIZE = 0x2000; //8k
  private static long copyAndClose(InputStream source, OutputStream sink) {
    long nread = 0L;
    byte[] buf = new byte[BUFFER_SIZE];
    int n;
    try {
      while ((n = source.read(buf)) > 0) {
        sink.write(buf, 0, n);
        nread += n;
      }
      sink.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return nread;
  }

  private static String readTextStream(InputStream inputStream) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    copyAndClose(inputStream, outputStream);
    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
  }

  public static String exec(String[] cmdarray, String stdinData) {
    try {
      Process process = Runtime.getRuntime().exec(cmdarray);
      try (OutputStream stdin = process.getOutputStream();
           InputStream stdout = process.getInputStream(); InputStream stderr = process.getErrorStream()) {
        CompletableFuture<Void> futureStdin = CompletableFuture.runAsync(()
            -> copyAndClose(new ByteArrayInputStream(stdinData.getBytes(StandardCharsets.UTF_8)), stdin));
        CompletableFuture<String> futureStdout = CompletableFuture.supplyAsync(() -> readTextStream(stdout));
        CompletableFuture<String> futureStderr = CompletableFuture.supplyAsync(() -> readTextStream(stderr));
        CompletableFuture.allOf(futureStdin, futureStdout, futureStderr).join();
        String stderrData = futureStderr.join();
        if (process.waitFor() != 0 || !stderrData.isEmpty()) {
          throw new IllegalStateException(stderrData);
        }
        return futureStdout.join();
      }
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

}
