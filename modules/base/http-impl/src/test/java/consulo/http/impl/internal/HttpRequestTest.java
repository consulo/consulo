/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.http.impl.internal;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import consulo.application.Application;
import consulo.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HttpRequestTest {
    private static final HttpRequestBuilderFactoryImpl FACTORY = new HttpRequestBuilderFactoryImpl(mock(Application.class));

    @Test
    public void getReadsBodyAndStatus() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("hello world"))) {
            String result = FACTORY.newBuilder(server.url("/data"), HttpMethod.GET)
                .useProxy(false)
                .connect(request -> {
                    assertThat(request.statusCode()).isEqualTo(200);
                    return request.readString(null);
                });

            assertThat(result).isEqualTo("hello world");
            assertThat(server.lastRequest().method).isEqualTo("GET");
            assertThat(server.lastRequest().path).isEqualTo("/data");
        }
    }

    @Test
    public void responseHeadersAreReadableCaseInsensitively() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> {
            StubHttpServer.Response response = StubHttpServer.Response.text("x");
            response.headers.put("X-Test", "yes");
            return response;
        })) {
            String value = FACTORY.newBuilder(server.url("/"), HttpMethod.GET)
                .useProxy(false)
                .connect(request -> request.headerValue("x-test"));

            assertThat(value).isEqualTo("yes");
        }
    }

    @Test
    public void requestHeadersAreSentUnchanged() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("ok"))) {
            FACTORY.newBuilder(server.url("/"), HttpMethod.GET)
                .useProxy(false)
                .header("X-Client", "consulo")
                .accept("application/json")
                .userAgent("consulo-agent/1.0")
                .readString(null);

            assertThat(server.lastRequest().headers.get("x-client")).isEqualTo("consulo");
            assertThat(server.lastRequest().headers.get("accept")).isEqualTo("application/json");
            assertThat(server.lastRequest().headers.get("user-agent")).isEqualTo("consulo-agent/1.0");
        }
    }

    @Test
    public void removedHeaderIsNotSent() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("ok"))) {
            FACTORY.newBuilder(server.url("/"), HttpMethod.GET)
                .useProxy(false)
                .header("X-Client", "consulo")
                .header("X-Client", null)
                .readString(null);

            assertThat(server.lastRequest().headers).doesNotContainKey("x-client");
        }
    }

    @Test
    public void postSendsBody() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("received"))) {
            String result = FACTORY.newBuilder(server.url("/submit"), HttpMethod.POST)
                .useProxy(false)
                .body("payload".getBytes(StandardCharsets.UTF_8))
                .readString(null);

            assertThat(result).isEqualTo("received");
            assertThat(server.lastRequest().method).isEqualTo("POST");
            assertThat(new String(server.lastRequest().body, StandardCharsets.UTF_8)).isEqualTo("payload");
        }
    }

    @Test
    public void gzipResponseIsDecoded() throws Exception {
        String payload = "compressed body value";
        try (StubHttpServer server = new StubHttpServer(request -> {
            StubHttpServer.Response response = new StubHttpServer.Response(200, gzip(payload.getBytes(StandardCharsets.UTF_8)));
            response.headers.put("Content-Type", "text/plain; charset=UTF-8");
            response.headers.put("Content-Encoding", "gzip");
            return response;
        })) {
            String result = FACTORY.newBuilder(server.url("/"), HttpMethod.GET)
                .useProxy(false)
                .gzip(true)
                .readString(null);

            assertThat(result).isEqualTo(payload);
        }
    }

    @Test
    public void errorCodeIsAvailableWhenAllowed() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> new StubHttpServer.Response(404, "missing".getBytes(StandardCharsets.UTF_8)))) {
            int status = FACTORY.newBuilder(server.url("/missing"), HttpMethod.GET)
                .useProxy(false)
                .allowErrorCodes(true)
                .connect(request -> request.statusCode());

            assertThat(status).isEqualTo(404);
        }
    }

    @Test
    public void saveToFileWritesToInMemoryFileSystem() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
             StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("downloaded content"))) {
            Path target = fs.getPath("/downloads/data.txt");

            Path saved = FACTORY.newBuilder(server.url("/file"), HttpMethod.GET)
                .useProxy(false)
                .connect(request -> request.saveToFile(target, null));

            assertThat(saved).isEqualTo(target);
            assertThat(Files.exists(target)).isTrue();
            assertThat(Files.readString(target)).isEqualTo("downloaded content");
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
        }
        return out.toByteArray();
    }
}
