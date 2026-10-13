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

import consulo.http.HttpMethod;
import consulo.http.HttpStatusException;
import consulo.http.HttpVersion;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * What goes on the wire and what comes back - the methods, bodies, headers, statuses and urls each executor can and
 * can not handle.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestExchangeTest extends HttpRequestTestCase {
    private static StubHttpServer answering(int status, String body) throws InterruptedException {
        return new StubHttpServer(request -> new StubHttpServer.Response(status, body.getBytes(StandardCharsets.UTF_8)));
    }

    @EachExecutorTest
    public void patch() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            if (isHttpClient()) {
                String result = request(server.url("/"), HttpMethod.PATCH).body("{}".getBytes(StandardCharsets.UTF_8)).readString(null);

                assertThat(result).isEqualTo("ok");
                assertThat(server.lastRequest().method).isEqualTo("PATCH");
                assertThat(new String(server.lastRequest().body, StandardCharsets.UTF_8)).isEqualTo("{}");
                return;
            }

            // HttpURLConnection knows no PATCH
            assertThatExceptionOfType(ProtocolException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.PATCH).readString(null));

            assertThat(server.requests()).isEmpty();
        }
    }

    @EachExecutorTest
    public void putDeleteHeadOptionsAreSent() throws Exception {
        try (StubHttpServer server = answering(200, "")) {
            for (HttpMethod method : new HttpMethod[]{HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS}) {
                request(server.url("/"), method).readString(null);
            }

            assertThat(server.requests()).extracting(r -> r.method).containsExactly("PUT", "DELETE", "HEAD", "OPTIONS");
        }
    }

    @EachExecutorTest
    public void getWithBody() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET)
                .body("payload".getBytes(StandardCharsets.UTF_8))
                .readString(null);

            // HttpURLConnection turns a GET with a body into a POST
            assertThat(server.lastRequest().method).isEqualTo(isHttpClient() ? "GET" : "POST");
            assertThat(new String(server.lastRequest().body, StandardCharsets.UTF_8)).isEqualTo("payload");
        }
    }

    @EachExecutorTest
    public void errorStatusThrowsWithStatusAndUrl() throws Exception {
        try (StubHttpServer server = answering(404, "missing")) {
            assertThatExceptionOfType(HttpStatusException.class)
                .isThrownBy(() -> request(server.url("/missing"), HttpMethod.GET).readString(null))
                .satisfies(e -> {
                    assertThat(e.getStatusCode()).isEqualTo(404);
                    assertThat(e.getUrl()).isEqualTo(server.url("/missing"));
                });
        }
    }

    @EachExecutorTest
    public void errorBodyWhenErrorCodesAreAllowed() throws Exception {
        try (StubHttpServer server = answering(404, "missing")) {
            if (isHttpClient()) {
                String body = request(server.url("/missing"), HttpMethod.GET)
                    .allowErrorCodes(true)
                    .connect(request -> {
                        assertThat(request.statusCode()).isEqualTo(404);
                        assertThat(request.statusMessage()).isEqualTo("Not Found");
                        return request.readString(null);
                    });

                assertThat(body).isEqualTo("missing");
                return;
            }

            // HttpURLConnection gives the body of an error status by getErrorStream() only
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/missing"), HttpMethod.GET)
                    .allowErrorCodes(true)
                    .connect(request -> {
                        assertThat(request.statusCode()).isEqualTo(404);
                        assertThat(request.statusMessage()).isEqualTo("Not Found");
                        return request.readString(null);
                    }));
        }
    }

    @EachExecutorTest
    public void notModifiedIsNotAnError() throws Exception {
        try (StubHttpServer server = answering(304, "")) {
            int status = request(server.url("/"), HttpMethod.GET).connect(request -> request.statusCode());

            assertThat(status).isEqualTo(304);
        }
    }

    @EachExecutorTest
    public void statusMessageIsTheReasonPhrase() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            String message = request(server.url("/"), HttpMethod.GET).connect(request -> request.statusMessage());

            assertThat(message).isEqualTo("OK");
        }
    }

    @EachExecutorTest
    public void version() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            HttpVersion version = request(server.url("/"), HttpMethod.GET)
                .version(HttpVersion.HTTP_2)
                .connect(request -> {
                    // not sent yet - the asked version, a constant for URLConnection
                    assertThat(request.version()).isEqualTo(isHttpClient() ? HttpVersion.HTTP_2 : HttpVersion.HTTP_1_1);
                    request.statusCode();
                    return request.version();
                });

            // the server can not upgrade to HTTP/2 - the answer is HTTP/1.1
            assertThat(version).isEqualTo(HttpVersion.HTTP_1_1);
            assertThat(server.lastRequest().protocol).isEqualTo("HTTP/1.1");
            if (isHttpClient()) {
                assertThat(server.lastRequest().headers).containsEntry("upgrade", "h2c");
            }
            else {
                assertThat(server.lastRequest().headers).doesNotContainKey("upgrade");
            }
        }
    }

    @EachExecutorTest
    public void noVersionIsHttp11() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET).readString(null);

            assertThat(server.lastRequest().protocol).isEqualTo("HTTP/1.1");
            assertThat(server.lastRequest().headers).doesNotContainKey("upgrade");
        }
    }

    @EachExecutorTest
    public void repeatedHeaderKeepsTheLastValue() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET)
                .header("X-Value", "first")
                .header("X-Value", "second")
                .readString(null);

            assertThat(server.lastRequest().headers).containsEntry("x-value", "second");
        }
    }

    @EachExecutorTest
    public void restrictedHeadersAreDropped() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET)
                .header("Host", "example.org")
                .header("Connection", "upgrade")
                .readString(null);

            assertThat(server.lastRequest().headers.get("host")).isEqualTo("127.0.0.1:" + server.port());
            assertThat(server.lastRequest().headers.get("connection")).isNotEqualTo("upgrade");
        }
    }

    @EachExecutorTest
    public void gzipIsAcceptedByDefault() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET).readString(null);
            assertThat(server.lastRequest().headers).containsEntry("accept-encoding", "gzip");

            request(server.url("/"), HttpMethod.GET).gzip(false).readString(null);
            assertThat(server.lastRequest().headers).doesNotContainKey("accept-encoding");
        }
    }

    @EachExecutorTest
    public void tryConnectReturnsTheStatus() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            assertThat(request(server.url("/"), HttpMethod.GET).tryConnect()).isEqualTo(200);
        }

        try (StubHttpServer server = answering(404, "missing")) {
            assertThatExceptionOfType(HttpStatusException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.GET).tryConnect());

            assertThat(request(server.url("/"), HttpMethod.GET).allowErrorCodes(true).tryConnect()).isEqualTo(404);
        }
    }

    /**
     * {@link java.net.http.HttpClient} sends http only - a file is read by URLConnection for both.
     */
    @EachExecutorTest
    public void fileUrlIsReadWithoutStatus(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("page.html");
        Files.writeString(file, "local content");
        String url = file.toUri().toString();

        String result = request(url, HttpMethod.GET).connect(request -> {
            assertThat(request.statusCode()).isEqualTo(0);
            return request.readString(null);
        });

        assertThat(result).isEqualTo("local content");
        assertThat(request(url, HttpMethod.GET).tryConnect()).isEqualTo(-1);
    }
}
