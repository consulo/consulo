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
import consulo.http.HttpProxyManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The proxy of a request comes from the {@link HttpProxyManager} the request api is given: URLConnection opens the
 * connection by it, {@link java.net.http.HttpClient} selects the proxy and logs in by it.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class HttpRequestProxyTest extends HttpRequestTestCase {
    private static final String PROXY_LOGIN =
        "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));

    private static Proxy httpProxy(StubHttpServer server) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", server.port()));
    }

    /**
     * Settings with the proxy the reference points to, logging in with the given login.
     */
    private static HttpProxyManager proxyManager(AtomicReference<Proxy> proxy, @Nullable PasswordAuthentication login) throws IOException {
        HttpProxyManager manager = mock(HttpProxyManager.class);

        when(manager.getProxySelector()).thenReturn(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(proxy.get());
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            }
        });

        // as HttpProxyManagerImpl opens it
        when(manager.openConnection(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return URI.create(url).toURL().openConnection(proxy.get());
        });

        when(manager.getProxyAuthenticator()).thenReturn(new Authenticator() {
            @Override
            protected @Nullable PasswordAuthentication getPasswordAuthentication() {
                return getRequestorType() == RequestorType.PROXY ? login : null;
            }
        });
        return manager;
    }

    private static HttpProxyManager proxyManager(Proxy proxy) throws IOException {
        return proxyManager(new AtomicReference<>(proxy), null);
    }

    @EachExecutorTest
    public void requestGoesThroughTheProxy() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("direct"));
             StubHttpServer proxy = new StubHttpServer(request -> StubHttpServer.Response.text("via proxy"))) {
            String result = factory(proxyManager(httpProxy(proxy))).newBuilder(target.url("/data"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("via proxy");
            // a proxy is asked for the whole url
            assertThat(proxy.lastRequest().path).isEqualTo(target.url("/data"));
            assertThat(target.requests()).isEmpty();
        }
    }

    @EachExecutorTest
    public void requestWithoutProxyDoesNotAskTheSettings() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("direct"));
             StubHttpServer proxy = new StubHttpServer(request -> StubHttpServer.Response.text("via proxy"))) {
            HttpProxyManager manager = proxyManager(httpProxy(proxy));

            String result = factory(manager).newBuilder(target.url("/data"), HttpMethod.GET).useProxy(false).readString(null);

            assertThat(result).isEqualTo("direct");
            assertThat(proxy.requests()).isEmpty();
            verifyNoInteractions(manager);
        }
    }

    @EachExecutorTest
    public void proxyLoginIsAnsweredByTheSettings() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("direct"));
             StubHttpServer proxy = new StubHttpServer(request -> {
                 if (PROXY_LOGIN.equals(request.headers.get("proxy-authorization"))) {
                     return StubHttpServer.Response.text("via proxy");
                 }
                 StubHttpServer.Response response = new StubHttpServer.Response(407, new byte[0]);
                 response.headers.put("Proxy-Authenticate", "Basic realm=\"consulo\"");
                 return response;
             })) {
            PasswordAuthentication login = new PasswordAuthentication("user", "secret".toCharArray());
            HttpProxyManager manager = proxyManager(new AtomicReference<>(httpProxy(proxy)), login);

            String result = factory(manager).newBuilder(target.url("/data"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("via proxy");
            assertThat(proxy.requests()).extracting(r -> r.headers.get("proxy-authorization")).containsExactly(null, PROXY_LOGIN);
        }
    }

    @EachExecutorTest
    public void changedSettingsApplyToTheNextRequest() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("direct"));
             StubHttpServer proxy = new StubHttpServer(request -> StubHttpServer.Response.text("via proxy"))) {
            AtomicReference<Proxy> current = new AtomicReference<>(httpProxy(proxy));
            HttpRequestBuilderFactoryImpl factory = factory(proxyManager(current, null));

            assertThat(factory.newBuilder(target.url("/"), HttpMethod.GET).readString(null)).isEqualTo("via proxy");

            current.set(Proxy.NO_PROXY);

            assertThat(factory.newBuilder(target.url("/"), HttpMethod.GET).readString(null)).isEqualTo("direct");
        }
    }

    /**
     * {@link java.net.http.HttpClient} connects around a SOCKS proxy - the request is opened by the settings.
     */
    @EachExecutorTest
    public void socksProxyIsOpenedByTheSettings() throws Exception {
        try (StubHttpServer target = new StubHttpServer(request -> StubHttpServer.Response.text("direct"))) {
            HttpProxyManager manager = proxyManager(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1)));
            // no SOCKS server here - the settings open a direct connection
            doAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return URI.create(url).toURL().openConnection(Proxy.NO_PROXY);
            }).when(manager).openConnection(anyString());

            String result = factory(manager).newBuilder(target.url("/data"), HttpMethod.GET).readString(null);

            assertThat(result).isEqualTo("direct");
            verify(manager).openConnection(target.url("/data"));
        }
    }
}
