// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.client;

import consulo.application.Application;
import consulo.http.HttpRequest;
import consulo.http.HttpRequestBuilder;
import consulo.logging.Logger;
import consulo.platform.Platform;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class HttpClientUtil {
    private static final Logger LOG = Logger.getInstance(HttpClientUtil.class);

    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    public static final String CONTENT_ENCODING_GZIP = "gzip";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String MIME_TYPE_TEXT_PREFIX = "text/";
    public static final String MIME_TYPE_APPLICATION_PREFIX = "application/";
    public static final String MIME_TYPE_JSON_SUFFIX = "+json";
    public static final String MIME_TYPE_XML_SUFFIX = "+xml";
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String MIME_TYPE_ENCODED_FORM = "application/x-www-form-urlencoded";

    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String ETAG_HEADER = "ETag";
    public static final String IF_NONE_MATCH_HEADER = "If-None-Match";

    private static final Set<String> PREDEFINED_TEXT_MIME_TYPES = Set.of(
        "application/xml", "application/x-yaml", "application/yaml", MIME_TYPE_JSON, "application/graphql",
        "application/atom+xml", "application/xhtml+xml", "application/svg+xml", "application/sql", MIME_TYPE_ENCODED_FORM,
        "text/plain", "text/xml", "text/html", "text/json", "text/csv",
        "image/svg+xml"
    );

    private HttpClientUtil() {
    }

    public static String getRequestName(String httpMethod, String url) {
        return "Request " + httpMethod + " " + url;
    }

    public static boolean isJsonMimeType(String mimeType) {
        String lc = mimeType.toLowerCase(Locale.ROOT);
        return lc.equals(MIME_TYPE_JSON) || lc.startsWith(MIME_TYPE_APPLICATION_PREFIX) && lc.endsWith(MIME_TYPE_JSON_SUFFIX);
    }

    /**
     * Build the User-Agent header value for the {@code agentName}
     * Append product, java and OS data
     */
    public static String getUserAgentValue(String agentName) {
        Application application = Application.get();
        String ideName = application.getName().get().replace(' ', '-');
        String ideBuild = application.isUnitTestMode() ? "test" : application.getBuildNumber().asString();
        Platform platform = Platform.current();
        String java = "JRE " + platform.jvm().rawVersion();
        String os = platform.os().name() + " " + platform.os().version();
        String arch = platform.os().arch();
        return agentName + " " + ideName + "/" + ideBuild + " (" + java + "; " + os + "; " + arch + ")";
    }

    /**
     * Set the {@code Accept} header to be {@code mimeType} if the header is missing
     */
    public static HttpRequestBuilder ensureAcceptHeader(HttpRequestBuilder request, String mimeType) {
        if (request.getHeader(ACCEPT_HEADER) == null) {
            request.accept(mimeType);
        }
        return request;
    }

    /**
     * Reads a response inside {@link HttpRequestBuilder#connect}: checks the status code, failing with
     * {@link HttpStatusErrorException} on an error one (&gt;=400) with the textual (depending on the {@code Content-Type}) response
     * body in {@link HttpStatusErrorException#getBody()}, then reads the body with {@code reader}.
     * <p>
     * Logs the request status code, and the textual response body when tracing is enabled in the logger. The request must allow
     * error codes, or an error status fails earlier without the body.
     */
    public static <T extends @Nullable Object> HttpApiResponse<T> readResponse(HttpRequest response,
                                                                               Logger logger,
                                                                               String requestName,
                                                                               HttpBodyReader<T> reader) throws IOException {
        int statusCode = response.statusCode();
        logger.debug(requestName + " : Status code " + statusCode);
        @Nullable HttpContentType contentType = getContentType(response);
        if (statusCode >= 400) {
            String errorBody;
            try {
                errorBody = readBodyAsText(response, contentType);
            }
            catch (IOException e) {
                logger.debug(requestName + " : Could not read the error response body", e);
                errorBody = null;
            }
            if (errorBody != null && logger.isTraceEnabled()) {
                logger.trace(requestName + " : Response body: " + errorBody);
            }
            throw new HttpStatusErrorException(requestName, statusCode, errorBody);
        }

        logger.debug(requestName + " : Reading response");
        T body;
        try (InputStream stream = response.getInputStream()) {
            body = readBody(stream, contentType, logger, requestName, reader);
        }
        logger.debug(requestName + " : Response extracted");
        return new HttpApiResponse<>(response.getURL(), statusCode, response.responseHeaders(), body);
    }

    private static <T extends @Nullable Object> T readBody(InputStream stream,
                                                           @Nullable HttpContentType contentType,
                                                           Logger logger,
                                                           String requestName,
                                                           HttpBodyReader<T> reader) throws IOException {
        if (logger.isTraceEnabled()) {
            if (contentType != null && isMimeTypeTextual(contentType.mimeType())) {
                byte[] bytes = stream.readAllBytes();
                Charset charset = contentType.charset() == null ? StandardCharsets.UTF_8 : contentType.charset();
                logger.trace(requestName + " : Response body: " + new String(bytes, charset));
                try (ByteArrayInputStream copy = new ByteArrayInputStream(bytes)) {
                    return reader.read(copy, contentType.mimeType(), contentType.charset());
                }
            }
            logger.trace(requestName + " : Non-textual content");
        }
        return reader.read(stream, contentType == null ? null : contentType.mimeType(), contentType == null ? null : contentType.charset());
    }

    /**
     * Cancels {@code source} once {@code result} is cancelled, so cancelling a chained future stops the request it waits for.
     */
    public static <T extends @Nullable Object> CompletableFuture<T> cancelWith(CompletableFuture<T> result, CompletableFuture<?> source) {
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                source.cancel(true);
            }
        });
        return result;
    }

    private static boolean isMimeTypeTextual(String mimeType) {
        String lc = mimeType.toLowerCase(Locale.ROOT);
        return lc.startsWith(MIME_TYPE_TEXT_PREFIX) && !lc.contains("stream")
            || PREDEFINED_TEXT_MIME_TYPES.contains(lc)
            || lc.startsWith(MIME_TYPE_APPLICATION_PREFIX) && lc.endsWith(MIME_TYPE_JSON_SUFFIX)
            || lc.startsWith(MIME_TYPE_APPLICATION_PREFIX) && lc.endsWith(MIME_TYPE_XML_SUFFIX);
    }

    private static @Nullable String readBodyAsText(HttpRequest response, @Nullable HttpContentType contentType) throws IOException {
        if (contentType == null || !isMimeTypeTextual(contentType.mimeType())) {
            return null;
        }
        Charset charset = contentType.charset() == null ? StandardCharsets.UTF_8 : contentType.charset();
        try (InputStream stream = response.getInputStream()) {
            return new String(stream.readAllBytes(), charset);
        }
    }

    private static @Nullable HttpContentType getContentType(HttpRequest response) throws IOException {
        String value = response.getContentType();
        if (value == null) {
            return null;
        }
        try {
            return HttpContentType.parse(value);
        }
        catch (Exception e) {
            LOG.debug("Could not parse " + CONTENT_TYPE_HEADER + " from value " + value, e);
            return null;
        }
    }
}
