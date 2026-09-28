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
package consulo.http.impl.internal.local;

import consulo.application.progress.ProgressIndicator;
import consulo.http.HttpCertificateManager;
import consulo.http.HttpMethod;
import consulo.http.HttpProxyManager;
import consulo.http.HttpRequestProcessor;
import consulo.http.HttpStatusException;
import consulo.http.HttpVersion;
import consulo.http.impl.internal.HttpRequestExecutor;
import consulo.http.impl.internal.HttpRequestOptions;
import consulo.http.localize.HttpLocalize;
import consulo.logging.Logger;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Sends the requests of the local platform with {@link HttpClient} - turned on by
 * {@link consulo.http.impl.internal.HttpRequestBuilderFactoryImpl#USE_HTTP_CLIENT_PROPERTY}.
 * <p>
 * It keeps what the {@link LocalUrlConnectionExecutor} does where callers depend on it: the redirect limit counts the
 * requests sent, a request without a body is sent once more when the connection is closed without an answer, the
 * headers {@link HttpClient} refuses to set are dropped, and a timeout is a {@link SocketTimeoutException}.
 * <p>
 * And it follows redirects the way a browser does: 301, 302, 303, 307 and 308, to a relative {@code Location} too, a
 * POST goes on as GET after 301, 302 and 303, and the credentials are not sent to another origin. PATCH is sent, a GET
 * keeps its body, the body of an error status can be read and the version is the one of the answer.
 * <p>
 * The proxy is the one {@link HttpProxyManager#getProxySelector()} selects, logged in by
 * {@link HttpProxyManager#getProxyAuthenticator()}.
 * <p>
 * What {@link HttpClient} can not do is left to the {@link LocalUrlConnectionExecutor}: a url which is not http, a
 * host name verifier, and a SOCKS proxy - {@link HttpClient} would connect around it.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public final class LocalHttpClientExecutor implements HttpRequestExecutor {
    private static final Logger LOG = Logger.getInstance(LocalHttpClientExecutor.class);

    /**
     * How long a wait lasts before the progress is checked for cancel.
     */
    static final long WAIT_SLICE_MS = 100;

    /**
     * The headers {@link HttpClient} refuses to set - {@link java.net.URLConnection} drops them silently.
     */
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");

    /**
     * The headers which are not sent again when a redirect goes to another origin.
     */
    private static final Set<String> CREDENTIAL_HEADERS = Set.of("authorization", "proxy-authorization", "cookie");

    record Exchange(HttpResponse<InputStream> response, String url) {
    }

    private record ClientKey(boolean https, boolean useProxy, int connectTimeout) {
    }

    private final HttpProxyManager myProxyManager;
    private final HttpCertificateManager myCertificateManager;
    private final LocalUrlConnectionExecutor myUrlConnectionExecutor;
    private final Supplier<@Nullable ProgressIndicator> myIndicator;

    private final Map<ClientKey, HttpClient> myClients = new ConcurrentHashMap<>();

    /**
     * @param urlConnectionExecutor sends what {@link HttpClient} can not
     * @param indicator             the progress of the current thread - a request stops when it is canceled
     */
    public LocalHttpClientExecutor(HttpProxyManager proxyManager,
                                   HttpCertificateManager certificateManager,
                                   LocalUrlConnectionExecutor urlConnectionExecutor,
                                   Supplier<@Nullable ProgressIndicator> indicator) {
        myProxyManager = proxyManager;
        myCertificateManager = certificateManager;
        myUrlConnectionExecutor = urlConnectionExecutor;
        myIndicator = indicator;
    }

    @Override
    public <T> T execute(HttpRequestOptions options, HttpRequestProcessor<T> processor) throws IOException {
        if (!canSend(options)) {
            return myUrlConnectionExecutor.execute(options, processor);
        }

        try (LocalHttpClientRequest request = new LocalHttpClientRequest(this, options)) {
            return processor.process(request);
        }
    }

    private boolean canSend(HttpRequestOptions options) {
        String url = options.url();
        if (!StringUtil.startsWithIgnoreCase(url, "http:") && !StringUtil.startsWithIgnoreCase(url, "https:")) {
            return false;
        }

        if (options.hostnameVerifier() != null) {
            return false;
        }

        URI uri;
        try {
            uri = new URI(url);
        }
        catch (URISyntaxException e) {
            // URLConnection sends a url which is not a valid URI, a space in the path for one
            return false;
        }

        if (options.useProxy()) {
            for (Proxy proxy : myProxyManager.getProxySelector().select(uri)) {
                if (proxy.type() == Proxy.Type.SOCKS) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sends the request and follows its redirects.
     */
    Exchange send(HttpRequestOptions options) throws IOException {
        String url = options.url();
        HttpMethod method = options.method();
        byte[] body = options.body();
        Map<String, String> headers = new LinkedHashMap<>(options.headers());

        for (int i = 0; i < options.redirectLimit(); i++) {
            if (options.forceHttps() && StringUtil.startsWith(url, "http:")) {
                url = "https:" + url.substring(5);
            }

            URI uri = toUri(url);

            HttpResponse<InputStream> response = sendWithRetry(options, uri, method, body, headers);

            int statusCode = response.statusCode();
            if (isRedirect(statusCode)) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location != null) {
                    StreamUtil.closeStream(response.body());

                    URI next = resolve(uri, location);

                    if ((statusCode == 303 && method != HttpMethod.HEAD)
                        || ((statusCode == 301 || statusCode == 302) && method == HttpMethod.POST)) {
                        method = HttpMethod.GET;
                        body = null;
                        headers.keySet().removeIf(name -> name.equalsIgnoreCase("Content-Type"));
                    }

                    if (!isSameOrigin(uri, next)) {
                        headers.keySet().removeIf(name -> CREDENTIAL_HEADERS.contains(name.toLowerCase(Locale.ROOT)));
                    }

                    url = next.toString();
                    continue;
                }
            }

            if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return new Exchange(response, url);
            }

            if (!options.allowErrorCodes() && (statusCode < 200 || statusCode >= 300)) {
                StreamUtil.closeStream(response.body());

                String message = HttpLocalize.errorConnectionFailedWithHttpCodeN(statusCode).get();

                throw new HttpStatusException(message, statusCode, url);
            }

            return new Exchange(response, url);
        }

        throw new IOException(HttpLocalize.errorConnectionFailedRedirects().get());
    }

    /**
     * As {@link HttpURLConnection} does: a request without a body is sent once more when the connection is closed before
     * an answer - not when it could not connect or timed out. {@link HttpClient} sends GET and HEAD once more itself.
     */
    private HttpResponse<InputStream> sendWithRetry(HttpRequestOptions options,
                                                    URI uri,
                                                    HttpMethod method,
                                                    byte @Nullable [] body,
                                                    Map<String, String> headers) throws IOException {
        try {
            return sendOnce(options, uri, method, body, headers);
        }
        catch (IOException e) {
            if (body != null
                || method == HttpMethod.GET
                || method == HttpMethod.HEAD
                || e instanceof SocketTimeoutException
                || e instanceof ConnectException
                || e instanceof SSLException
                || e instanceof InterruptedIOException) {
                throw e;
            }

            LOG.debug("Sending again: " + uri, e);
            return sendOnce(options, uri, method, body, headers);
        }
    }

    private HttpResponse<InputStream> sendOnce(HttpRequestOptions options,
                                               URI uri,
                                               HttpMethod method,
                                               byte @Nullable [] body,
                                               Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .method(method.name(), body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body))
            .version(toClientVersion(options.version()));

        if (options.readTimeout() > 0) {
            builder.timeout(Duration.ofMillis(options.readTimeout()));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (RESTRICTED_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                LOG.debug("Header is not sent: " + entry.getKey());
                continue;
            }
            builder.header(entry.getKey(), entry.getValue());
        }

        if (options.gzip()) {
            builder.setHeader("Accept-Encoding", "gzip");
        }

        HttpClient client = getClient(options, uri);

        CompletableFuture<HttpResponse<InputStream>> future =
            client.sendAsync(builder.build(), info -> new LocalHttpClientBodyStream(options.readTimeout(), myIndicator));

        return await(future);
    }

    /**
     * Waits for the answer in slices, so a canceled progress stops the wait.
     */
    private <R> R await(CompletableFuture<R> future) throws IOException {
        while (true) {
            try {
                return future.get(WAIT_SLICE_MS, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e) {
                ProgressIndicator indicator = myIndicator.get();
                if (indicator != null && indicator.isCanceled()) {
                    future.cancel(true);
                    indicator.checkCanceled();
                }
            }
            catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            catch (ExecutionException e) {
                throw rethrow(e.getCause());
            }
        }
    }

    private static IOException rethrow(@Nullable Throwable cause) {
        if (cause instanceof HttpConnectTimeoutException) {
            SocketTimeoutException exception = new SocketTimeoutException("Connect timed out");
            exception.initCause(cause);
            return exception;
        }

        if (cause instanceof HttpTimeoutException) {
            SocketTimeoutException exception = new SocketTimeoutException("Read timed out");
            exception.initCause(cause);
            return exception;
        }

        if (cause instanceof IOException e) {
            return e;
        }

        if (cause instanceof RuntimeException e) {
            throw e;
        }

        if (cause instanceof Error e) {
            throw e;
        }

        return new IOException(cause);
    }

    private HttpClient getClient(HttpRequestOptions options, URI uri) {
        ClientKey key = new ClientKey("https".equalsIgnoreCase(uri.getScheme()), options.useProxy(), options.connectTimeout());
        return myClients.computeIfAbsent(key, this::createClient);
    }

    private HttpClient createClient(ClientKey key) {
        HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER);

        if (key.connectTimeout() > 0) {
            builder.connectTimeout(Duration.ofMillis(key.connectTimeout()));
        }

        if (key.useProxy()) {
            // asked on each request - a change of the proxy settings applies to the clients made already
            builder.proxy(myProxyManager.getProxySelector());
            builder.authenticator(myProxyManager.getProxyAuthenticator());
        }
        else {
            builder.proxy(HttpClient.Builder.NO_PROXY);
        }

        // only a https client asks for the certificates
        if (key.https()) {
            builder.sslContext(myCertificateManager.getSslContext());
        }

        return builder.build();
    }

    /**
     * No version asked is HTTP/1.1, as {@link java.net.URLConnection} sends - the default of {@link HttpClient} is
     * HTTP/2, which asks a plain server to upgrade.
     */
    private static HttpClient.Version toClientVersion(@Nullable HttpVersion version) {
        if (version == null) {
            return HttpClient.Version.HTTP_1_1;
        }

        return switch (version) {
            case HTTP_1_1 -> HttpClient.Version.HTTP_1_1;
            case HTTP_2 -> HttpClient.Version.HTTP_2;
            case HTTP_3 -> {
                try {
                    yield HttpClient.Version.valueOf("HTTP_3");
                }
                catch (IllegalArgumentException e) {
                    // the runtime has no HTTP/3 yet
                    yield HttpClient.Version.HTTP_2;
                }
            }
        };
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private static boolean isSameOrigin(URI from, URI to) {
        return StringUtil.equalsIgnoreCase(from.getScheme(), to.getScheme())
            && StringUtil.equalsIgnoreCase(from.getHost(), to.getHost())
            && port(from) == port(to);
    }

    private static int port(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static URI toUri(String url) throws MalformedURLException {
        try {
            return new URI(url);
        }
        catch (URISyntaxException e) {
            MalformedURLException exception = new MalformedURLException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    private static URI resolve(URI uri, String location) throws MalformedURLException {
        try {
            return uri.resolve(location);
        }
        catch (IllegalArgumentException e) {
            MalformedURLException exception = new MalformedURLException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }
}
