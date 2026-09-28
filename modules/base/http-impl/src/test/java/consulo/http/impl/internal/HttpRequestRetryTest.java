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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * When a request is sent again today. The builder has no retry of its own - the only retry is the one
 * {@link java.net.HttpURLConnection} does by itself: a request without a body is sent once more when the server closes
 * the connection without an answer. A request with a body is streamed and never sent again.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestRetryTest {
    private static final HttpRequestBuilderFactoryImpl FACTORY = new HttpRequestBuilderFactoryImpl(mock(Application.class));

    private static HttpRequestBuilder request(String url, HttpMethod method) {
        return FACTORY.newBuilder(url, method).useProxy(false);
    }

    /**
     * Closes the connection without an answer for the first {@code drops} requests, then answers.
     */
    private static StubHttpServer dropping(int drops) throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        return new StubHttpServer(request -> count.incrementAndGet() <= drops ? null : StubHttpServer.Response.text("answered"));
    }

    @Test
    public void getIsSentOnceMoreAfterADroppedConnection() throws Exception {
        try (StubHttpServer server = dropping(1)) {
            String result = request(server.url("/"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("answered");
            assertThat(server.requests()).hasSize(2);
        }
    }

    @Test
    public void getFailsWhenTheSecondConnectionIsDroppedToo() throws Exception {
        try (StubHttpServer server = dropping(2)) {
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.GET).readString(null));

            assertThat(server.requests()).hasSize(2);
        }
    }

    @Test
    public void postWithoutBodyIsSentOnceMore() throws Exception {
        try (StubHttpServer server = dropping(1)) {
            String result = request(server.url("/"), HttpMethod.POST).readString(null);

            assertThat(result).isEqualTo("answered");
            assertThat(server.requests()).hasSize(2);
        }
    }

    @Test
    public void requestWithBodyIsNotSentAgain() throws Exception {
        try (StubHttpServer server = dropping(1)) {
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.POST)
                    .body("payload".getBytes(StandardCharsets.UTF_8))
                    .readString(null));

            assertThat(server.requests()).hasSize(1);
        }
    }

    @Test
    public void errorStatusIsNotSentAgain() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> new StubHttpServer.Response(503, new byte[0]))) {
            assertThatExceptionOfType(HttpStatusException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.GET).readString(null))
                .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(503));

            assertThat(server.requests()).hasSize(1);
        }
    }

    @Test
    public void readTimeoutIsNotSentAgain() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> {
            Thread.sleep(1500);
            return StubHttpServer.Response.text("late");
        })) {
            assertThatExceptionOfType(SocketTimeoutException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.GET).readTimeout(300).readString(null));

            // the server works on one thread - a second request would be recorded after the first answer
            Thread.sleep(2000);
            assertThat(server.requests()).hasSize(1);
        }
    }
}
