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

import consulo.application.Application;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilder;
import consulo.http.HttpStatusException;
import consulo.http.HttpVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * What goes on the wire and what comes back today, by the {@link java.net.URLConnection} implementation - the methods,
 * bodies, headers, statuses and urls it can and can not handle.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestExchangeTest {
    private static final HttpRequestBuilderFactoryImpl FACTORY = new HttpRequestBuilderFactoryImpl(mock(Application.class));

    private static HttpRequestBuilder request(String url, HttpMethod method) {
        return FACTORY.newBuilder(url, method).useProxy(false);
    }

    private static StubHttpServer answering(int status, String body) throws InterruptedException {
        return new StubHttpServer(request -> new StubHttpServer.Response(status, body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void patchIsRejectedBeforeSending() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            assertThatExceptionOfType(ProtocolException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.PATCH).readString(null));

            assertThat(server.requests()).isEmpty();
        }
    }

    @Test
    public void putDeleteHeadOptionsAreSent() throws Exception {
        try (StubHttpServer server = answering(200, "")) {
            for (HttpMethod method : new HttpMethod[]{HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS}) {
                request(server.url("/"), method).readString(null);
            }

            assertThat(server.requests()).extracting(r -> r.method).containsExactly("PUT", "DELETE", "HEAD", "OPTIONS");
        }
    }

    @Test
    public void getWithBodyIsSentAsPost() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET)
                .body("payload".getBytes(StandardCharsets.UTF_8))
                .readString(null);

            assertThat(server.lastRequest().method).isEqualTo("POST");
            assertThat(new String(server.lastRequest().body, StandardCharsets.UTF_8)).isEqualTo("payload");
        }
    }

    @Test
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

    @Test
    public void errorBodyCanNotBeReadEvenWhenErrorCodesAreAllowed() throws Exception {
        try (StubHttpServer server = answering(404, "missing")) {
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

    @Test
    public void notModifiedIsNotAnError() throws Exception {
        try (StubHttpServer server = answering(304, "")) {
            int status = request(server.url("/"), HttpMethod.GET).connect(request -> request.statusCode());

            assertThat(status).isEqualTo(304);
        }
    }

    @Test
    public void statusMessageIsTheReasonPhrase() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            String message = request(server.url("/"), HttpMethod.GET).connect(request -> request.statusMessage());

            assertThat(message).isEqualTo("OK");
        }
    }

    @Test
    public void versionIsAlwaysHttp11() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            HttpVersion version = request(server.url("/"), HttpMethod.GET)
                .version(HttpVersion.HTTP_2)
                .connect(request -> {
                    // version() is a constant - it does not even connect
                    assertThat(request.version()).isEqualTo(HttpVersion.HTTP_1_1);
                    request.statusCode();
                    return request.version();
                });

            assertThat(version).isEqualTo(HttpVersion.HTTP_1_1);
            assertThat(server.lastRequest().protocol).isEqualTo("HTTP/1.1");
        }
    }

    @Test
    public void repeatedHeaderKeepsTheLastValue() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET)
                .header("X-Value", "first")
                .header("X-Value", "second")
                .readString(null);

            assertThat(server.lastRequest().headers).containsEntry("x-value", "second");
        }
    }

    @Test
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

    @Test
    public void gzipIsAcceptedByDefault() throws Exception {
        try (StubHttpServer server = answering(200, "ok")) {
            request(server.url("/"), HttpMethod.GET).readString(null);
            assertThat(server.lastRequest().headers).containsEntry("accept-encoding", "gzip");

            request(server.url("/"), HttpMethod.GET).gzip(false).readString(null);
            assertThat(server.lastRequest().headers).doesNotContainKey("accept-encoding");
        }
    }

    @Test
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

    @Test
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
