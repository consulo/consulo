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

import consulo.http.HttpCertificateManager;
import consulo.http.HttpProxyManager;
import consulo.http.HttpRequestProcessor;
import consulo.http.HttpStatusException;
import consulo.http.impl.internal.HttpRequestExecutor;
import consulo.http.impl.internal.HttpRequestOptions;
import consulo.http.localize.HttpLocalize;
import consulo.util.lang.StringUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Sends the requests of the local platform with {@link URLConnection} - the default executor.
 * <p>
 * Redirects are followed by hand: 301 and 302 only, each hop with the same method, body and headers, to an absolute
 * {@code Location} only. The redirect limit counts the requests sent. {@link HttpURLConnection} itself sends a request
 * without a body once more when the connection is closed without an answer.
 * <p>
 * A request through the proxy is opened by {@link HttpProxyManager#openConnection(String)}.
 *
 * @author VISTALL
 * @since 2026-02-13
 */
public final class LocalUrlConnectionExecutor implements HttpRequestExecutor {
    private final HttpProxyManager myProxyManager;
    private final HttpCertificateManager myCertificateManager;

    public LocalUrlConnectionExecutor(HttpProxyManager proxyManager, HttpCertificateManager certificateManager) {
        myProxyManager = proxyManager;
        myCertificateManager = certificateManager;
    }

    @Override
    public <T> T execute(HttpRequestOptions options, HttpRequestProcessor<T> processor) throws IOException {
        try (LocalUrlConnectionRequest request = new LocalUrlConnectionRequest(this, options)) {
            return processor.process(request);
        }
    }

    URLConnection openConnection(HttpRequestOptions options) throws IOException {
        String url = options.url();

        for (int i = 0; i < options.redirectLimit(); i++) {
            if (options.forceHttps() && StringUtil.startsWith(url, "http:")) {
                url = "https:" + url.substring(5);
            }

            URLConnection connection;
            if (!options.useProxy()) {
                connection = new URL(url).openConnection(Proxy.NO_PROXY);
            }
            else {
                connection = myProxyManager.openConnection(url);

                if (connection instanceof HttpURLConnection httpURLConnection) {
                    httpURLConnection.setAuthenticator(myProxyManager.getProxyAuthenticator());
                }
            }

            if (connection instanceof HttpURLConnection httpURLConnection) {
                // we will control redirection by code lower
                httpURLConnection.setInstanceFollowRedirects(false);

                // set method from builder
                httpURLConnection.setRequestMethod(options.method().name());
            }

            for (Map.Entry<String, String> entry : options.headers().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (connection instanceof HttpsURLConnection httpsURLConnection) {
                httpsURLConnection.setSSLSocketFactory(myCertificateManager.getSslContext().getSocketFactory());
            }

            connection.setConnectTimeout(options.connectTimeout());
            connection.setReadTimeout(options.readTimeout());

            if (options.hostnameVerifier() != null && connection instanceof HttpsURLConnection httpsURLConnection) {
                httpsURLConnection.setHostnameVerifier(options.hostnameVerifier());
            }

            if (options.gzip()) {
                connection.setRequestProperty("Accept-Encoding", "gzip");
            }

            connection.setUseCaches(false);

            byte[] body = options.body();
            if (body != null) {
                connection.setDoOutput(true);

                if (connection instanceof HttpURLConnection httpURLConnection) {
                    httpURLConnection.setFixedLengthStreamingMode(body.length);
                }

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body);
                    os.flush();
                }
            }

            if (connection instanceof HttpURLConnection httpURLConnection) {
                int responseCode = httpURLConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    httpURLConnection.disconnect();

                    url = connection.getHeaderField("Location");
                    if (url != null) {
                        continue;
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    httpURLConnection.disconnect();
                    return connection;
                }

                if (!options.allowErrorCodes()) {
                    if (responseCode < 200 || responseCode >= 300) {
                        httpURLConnection.disconnect();

                        String message = HttpLocalize.errorConnectionFailedWithHttpCodeN(responseCode).get();

                        throw new HttpStatusException(message, responseCode, StringUtil.notNullize(url, "Empty URL"));
                    }
                }
            }

            return connection;
        }

        throw new IOException(HttpLocalize.errorConnectionFailedRedirects().get());
    }
}
