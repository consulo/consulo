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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * How redirects are followed today, by the {@link java.net.URLConnection} implementation: the loop in
 * {@link HttpRequestBuilderFactoryImpl#openConnection} follows 301 and 302 only, sends every hop with the same method,
 * body and headers, and counts the requests it sends against the redirect limit.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestRedirectTest {
    private static final HttpRequestBuilderFactoryImpl FACTORY = new HttpRequestBuilderFactoryImpl(mock(Application.class));

    private static HttpRequestBuilder request(String url, HttpMethod method) {
        return FACTORY.newBuilder(url, method).useProxy(false);
    }

    /**
     * An absolute url of the server the request came to.
     */
    private static String sameServer(StubHttpServer.RecordedRequest request, String path) {
        return "http://" + request.headers.get("host") + path;
    }

    /**
     * {@code /start} answers with the given status and an absolute {@code Location} of {@code /target}.
     */
    private static StubHttpServer redirectToTarget(int status) throws InterruptedException {
        return new StubHttpServer(request -> request.path.equals("/start")
            ? StubHttpServer.Response.redirect(status, sameServer(request, "/target"))
            : StubHttpServer.Response.text("arrived"));
    }

    /**
     * {@code /start} answers with the given response.
     */
    private static StubHttpServer startAnswers(StubHttpServer.Response response) throws InterruptedException {
        return new StubHttpServer(request -> request.path.equals("/start") ? response : StubHttpServer.Response.text("arrived"));
    }

    /**
     * {@code /hop/N} redirects to {@code /hop/N-1}, {@code /hop/0} answers.
     */
    private static StubHttpServer hops(int status) throws InterruptedException {
        return new StubHttpServer(request -> {
            int hop = Integer.parseInt(request.path.substring("/hop/".length()));
            return hop == 0
                ? StubHttpServer.Response.text("arrived")
                : StubHttpServer.Response.redirect(status, sameServer(request, "/hop/" + (hop - 1)));
        });
    }

    @Test
    public void movedPermanentlyIsFollowed() throws Exception {
        try (StubHttpServer server = redirectToTarget(301)) {
            String result = request(server.url("/start"), HttpMethod.GET).connect(request -> {
                assertThat(request.statusCode()).isEqualTo(200);
                return request.readString(null);
            });

            assertThat(result).isEqualTo("arrived");
            assertThat(server.requests()).extracting(r -> r.path).containsExactly("/start", "/target");
        }
    }

    @Test
    public void foundIsFollowed() throws Exception {
        try (StubHttpServer server = redirectToTarget(302)) {
            String result = request(server.url("/start"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("arrived");
            assertThat(server.requests()).extracting(r -> r.path).containsExactly("/start", "/target");
        }
    }

    @Test
    public void urlIsTheRequestedOneUntilConnected() throws Exception {
        try (StubHttpServer server = redirectToTarget(302)) {
            List<String> urls = request(server.url("/start"), HttpMethod.GET).connect(request -> {
                String before = request.getURL();
                request.statusCode();
                return List.of(before, request.getURL());
            });

            assertThat(urls).containsExactly(server.url("/start"), server.url("/target"));
        }
    }

    @Test
    public void seeOtherIsNotFollowed() throws Exception {
        assertNotFollowed(303);
    }

    @Test
    public void temporaryRedirectIsNotFollowed() throws Exception {
        assertNotFollowed(307);
    }

    @Test
    public void permanentRedirectIsNotFollowed() throws Exception {
        assertNotFollowed(308);
    }

    private static void assertNotFollowed(int status) throws Exception {
        try (StubHttpServer server = redirectToTarget(status)) {
            assertThatExceptionOfType(HttpStatusException.class)
                .isThrownBy(() -> request(server.url("/start"), HttpMethod.GET).readString(null))
                .satisfies(e -> {
                    assertThat(e.getStatusCode()).isEqualTo(status);
                    assertThat(e.getUrl()).isEqualTo(server.url("/start"));
                });

            String location = request(server.url("/start"), HttpMethod.GET)
                .allowErrorCodes(true)
                .connect(request -> {
                    assertThat(request.statusCode()).isEqualTo(status);
                    return request.headerValue("Location");
                });

            assertThat(location).isEqualTo(server.url("/target"));
            assertThat(server.requests()).extracting(r -> r.path).containsExactly("/start", "/start");
        }
    }

    @Test
    public void redirectSendsTheSameMethodAndBodyAgain() throws Exception {
        try (StubHttpServer server = redirectToTarget(302)) {
            request(server.url("/start"), HttpMethod.POST)
                .body("payload".getBytes(StandardCharsets.UTF_8))
                .readString(null);

            // a browser sends GET without the body after 302 - URLConnection repeats the POST
            assertThat(server.requests()).hasSize(2);
            StubHttpServer.RecordedRequest redirected = server.requests().get(1);
            assertThat(redirected.path).isEqualTo("/target");
            assertThat(redirected.method).isEqualTo("POST");
            assertThat(new String(redirected.body, StandardCharsets.UTF_8)).isEqualTo("payload");
        }
    }

    @Test
    public void redirectToAnotherOriginSendsTheHeadersAgain() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("arrived"));
             StubHttpServer origin = startAnswers(StubHttpServer.Response.redirect(302, target.url("/target")))) {
            request(origin.url("/start"), HttpMethod.GET)
                .header("Authorization", "Bearer secret")
                .header("X-Client", "consulo")
                .readString(null);

            assertThat(target.requests()).hasSize(1);
            assertThat(target.lastRequest().headers)
                .containsEntry("authorization", "Bearer secret")
                .containsEntry("x-client", "consulo");
        }
    }

    @Test
    public void relativeLocationFails() throws Exception {
        try (StubHttpServer server = startAnswers(StubHttpServer.Response.redirect(302, "/target"))) {
            assertThatExceptionOfType(MalformedURLException.class)
                .isThrownBy(() -> request(server.url("/start"), HttpMethod.GET).readString(null));

            assertThat(server.requests()).extracting(r -> r.path).containsExactly("/start");
        }
    }

    @Test
    public void redirectWithoutLocationIsAnErrorStatus() throws Exception {
        try (StubHttpServer server = startAnswers(new StubHttpServer.Response(302, new byte[0]))) {
            assertThatExceptionOfType(HttpStatusException.class)
                .isThrownBy(() -> request(server.url("/start"), HttpMethod.GET).readString(null))
                .satisfies(e -> {
                    assertThat(e.getStatusCode()).isEqualTo(302);
                    assertThat(e.getUrl()).isEqualTo("Empty URL");
                });

            int status = request(server.url("/start"), HttpMethod.GET)
                .allowErrorCodes(true)
                .connect(request -> request.statusCode());

            assertThat(status).isEqualTo(302);
        }
    }

    @Test
    public void defaultLimitAllowsNineRedirects() throws Exception {
        try (StubHttpServer server = hops(302)) {
            String result = request(server.url("/hop/9"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("arrived");
            assertThat(server.requests()).hasSize(10);
        }
    }

    @Test
    public void tenthRedirectExceedsTheDefaultLimit() throws Exception {
        try (StubHttpServer server = hops(301)) {
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/hop/10"), HttpMethod.GET).readString(null))
                .isNotInstanceOf(HttpStatusException.class);

            assertThat(server.requests()).hasSize(10);
        }
    }

    @Test
    public void limitCountsTheRequestsSent() throws Exception {
        try (StubHttpServer server = hops(302)) {
            String result = request(server.url("/hop/1"), HttpMethod.GET).redirectLimit(2).readString(null);
            assertThat(result).isEqualTo("arrived");

            server.requests().clear();

            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/hop/2"), HttpMethod.GET).redirectLimit(2).readString(null))
                .isNotInstanceOf(HttpStatusException.class);
            assertThat(server.requests()).extracting(r -> r.path).containsExactly("/hop/2", "/hop/1");
        }
    }

    @Test
    public void limitOfOneReturnsNoRedirectEvenWhenErrorCodesAreAllowed() throws Exception {
        try (StubHttpServer server = hops(302)) {
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/hop/1"), HttpMethod.GET)
                    .redirectLimit(1)
                    .allowErrorCodes(true)
                    .connect(request -> request.statusCode()))
                .isNotInstanceOf(HttpStatusException.class);

            assertThat(server.requests()).hasSize(1);
        }
    }

    @Test
    public void limitOfZeroSendsNothing() throws Exception {
        try (StubHttpServer server = new StubHttpServer(request -> StubHttpServer.Response.text("ok"))) {
            assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> request(server.url("/"), HttpMethod.GET).redirectLimit(0).readString(null));

            assertThat(server.requests()).isEmpty();
        }
    }
}
