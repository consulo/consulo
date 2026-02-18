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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.http.*;
import consulo.http.localize.HttpLocalize;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@ServiceImpl
@Singleton
public class HttpRequestBuilderFactoryImpl implements HttpRequestBuilderFactory {
    private static final Logger LOG = Logger.getInstance(HttpRequests.class);

    static final int BLOCK_SIZE = 16 * 1024;

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([^;]+)");

    static <T> T process(HttpRequestBuilderImpl builder, HttpRequestProcessor<T> processor) throws IOException {
        LOG.assertTrue(ApplicationManager.getApplication() == null || !ApplicationManager.getApplication().isReadAccessAllowed(), "Network shouldn't be accessed in EDT or inside read action");

        return doProcess(builder, processor);
    }

    private static <T> T doProcess(HttpRequestBuilderImpl builder, HttpRequestProcessor<T> processor) throws IOException {
        try (HttpRequestImpl request = new HttpRequestImpl(builder)) {
            return processor.process(request);
        }
    }

    static Charset getCharset(HttpRequestImpl request) throws IOException {
        String contentType = request.getConnection().getContentType();
        if (!StringUtil.isEmptyOrSpaces(contentType)) {
            Matcher m = CHARSET_PATTERN.matcher(contentType);
            if (m.find()) {
                try {
                    return Charset.forName(StringUtil.unquoteString(m.group(1)));
                }
                catch (IllegalArgumentException e) {
                    throw new IOException("unknown charset (" + contentType + ")", e);
                }
            }
        }

        return StandardCharsets.UTF_8;
    }

    static URLConnection openConnection(HttpRequestBuilderImpl builder) throws IOException {
        String url = builder.myUrl;

        for (int i = 0; i < builder.myRedirectLimit; i++) {
            if (builder.myForceHttps && StringUtil.startsWith(url, "http:")) {
                url = "https:" + url.substring(5);
            }

            URLConnection connection;
            if (!builder.myUseProxy) {
                connection = new URL(url).openConnection(Proxy.NO_PROXY);
            }
            else {
                connection = HttpProxyManager.getInstance().openConnection(url);
            }

            if (builder.myBody != null) {
                connection.setDoOutput(true);

                if (connection instanceof HttpURLConnection httpURLConnection) {
                    httpURLConnection.setFixedLengthStreamingMode(builder.myBody.length);
                }
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(builder.myBody);
                    os.flush();
                }
            }

            if (connection instanceof HttpURLConnection httpURLConnection) {
                // we will control redirection by code lower
                httpURLConnection.setInstanceFollowRedirects(false);

                // set method from builder
                httpURLConnection.setRequestMethod(builder.myHttpMethod.name());
            }

            for (Map.Entry<String, String> entry : builder.myHeaders.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (connection instanceof HttpsURLConnection httpsURLConnection) {
                httpsURLConnection.setSSLSocketFactory(HttpCertificateManager.getInstance().getSslContext().getSocketFactory());
            }

            connection.setConnectTimeout(builder.myConnectTimeout);
            connection.setReadTimeout(builder.myTimeout);


            if (builder.myHostnameVerifier != null && connection instanceof HttpsURLConnection httpsURLConnection) {
                httpsURLConnection.setHostnameVerifier(builder.myHostnameVerifier);
            }

            if (builder.myGzip) {
                connection.setRequestProperty("Accept-Encoding", "gzip");
            }

            connection.setUseCaches(false);

            if (connection instanceof HttpURLConnection httpURLConnection) {
                int responseCode = httpURLConnection.getResponseCode();

                if (responseCode < 200 || responseCode >= 300 && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    httpURLConnection.disconnect();

                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                        url = connection.getHeaderField("Location");
                        if (url != null) {
                            continue;
                        }
                    }

                    String message = HttpLocalize.errorConnectionFailedWithHttpCodeN(responseCode).get();
                    throw new HttpStatusException(message, responseCode, StringUtil.notNullize(url, "Empty URL"));
                }
            }

            return connection;
        }

        throw new IOException(HttpLocalize.errorConnectionFailedRedirects().get());
    }

    @Nonnull
    public static String createErrorMessage(@Nonnull IOException e, @Nonnull HttpRequest request, boolean includeHeaders) {
        StringBuilder builder = new StringBuilder();

        builder.append("Cannot download '").append(request.getURL()).append("': ").append(e.getMessage());

        try {
            URLConnection connection = ((HttpRequestImpl) request).getConnection();

            if (includeHeaders) {
                builder.append("\n, headers: ").append(connection.getHeaderFields());
            }

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                builder.append("\n, response: ").append(httpConnection.getResponseCode()).append(' ').append(httpConnection.getResponseMessage());
            }
        }
        catch (Throwable ignored) {
        }

        return builder.toString();
    }

    private final Application myApplication;

    @Inject
    public HttpRequestBuilderFactoryImpl(Application application) {
        myApplication = application;
    }

    @Nonnull
    @Override
    public HttpRequestBuilder newBuilder(@Nonnull String url, @Nonnull HttpMethod httpMethod) {
        return new HttpRequestBuilderImpl(myApplication, url, httpMethod);
    }
}
