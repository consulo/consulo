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
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import org.apache.http.entity.ContentType;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPInputStream;

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

    public static String getRequestName(String httpMethod, URI uri) {
        return "Request " + httpMethod + " " + uri;
    }

    public static String getRequestName(HttpRequest request) {
        return getRequestName(request.method(), request.uri());
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
     * Override the {@code Accept-Encoding} header to be {@code gzip}
     */
    public static HttpRequest forceAcceptGzipEncoding(HttpRequest request) {
        return HttpRequest.newBuilder(request, (name, value) ->
                // only allow gzip encoding
                !name.equalsIgnoreCase(ACCEPT_ENCODING_HEADER) || value.equalsIgnoreCase(CONTENT_ENCODING_GZIP))
            .header(ACCEPT_ENCODING_HEADER, CONTENT_ENCODING_GZIP)
            .build();
    }

    /**
     * Set the {@code Accept} header to be {@code mimeType} if the header is missing
     */
    public static HttpRequest ensureAcceptHeader(HttpRequest request, String mimeType) {
        if (request.headers().firstValue(ACCEPT_HEADER).isPresent()) {
            return request;
        }
        return HttpRequest.newBuilder(request, (name, value) -> true).header(ACCEPT_HEADER, mimeType).build();
    }

    /**
     * A blocking delegate of {@link #checkStatusCodeWithLogging}.
     * <p>
     * NB: Can be interrupted. {@link HttpStatusErrorException#getBody()} will be {@code null} in case of interrupt
     */
    public static void checkStatusCodeWithLoggingBlocking(HttpResponse<? extends InputStream> response, Logger logger, String requestName) {
        int statusCode = response.statusCode();
        logger.debug(requestName + " : Status code " + statusCode);
        if (statusCode >= 400) {
            String errorBody;
            try {
                errorBody = readBodyAsText(response);
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
    }

    /**
     * Checks the status code of the response and fails with {@link HttpStatusErrorException} if status code is not a successful one
     * (&gt;=400). Reads the textual (depending on the {@code Content-Type}) response body into {@link HttpStatusErrorException#getBody()}
     * <p>
     * Logs request status code and also response body if tracing is enabled in logger and the response body is textual
     * according to content type header
     */
    public static CompletableFuture<?> checkStatusCodeWithLogging(HttpResponse<? extends InputStream> response, Logger logger, String requestName) {
        return supplyIO(() -> {
            checkStatusCodeWithLoggingBlocking(response, logger, requestName);
            return null;
        });
    }

    /**
     * A blocking version of {@link #readBodyWithLogging}.
     */
    public static <T extends @Nullable Object> T readBodyWithLoggingBlocking(HttpResponse<? extends InputStream> response,
                                                                             Logger logger,
                                                                             String requestName,
                                                                             HttpBodyReader<T> reader) throws IOException {
        logger.debug(requestName + " : Reading response");
        @Nullable HttpContentType contentType = getContentType(response);
        T result = readBodyInflating(response, stream -> {
            if (logger.isTraceEnabled()) {
                if (contentType != null && isMimeTypeTextual(contentType.mimeType())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    stream.transferTo(out);
                    byte[] bytes = out.toByteArray();
                    Charset charset = contentType.charset() == null ? StandardCharsets.UTF_8 : contentType.charset();
                    logger.trace(requestName + " : Response body: " + new String(bytes, charset));
                    try (ByteArrayInputStream copy = new ByteArrayInputStream(bytes)) {
                        return reader.read(copy, contentType.mimeType(), contentType.charset());
                    }
                }
                logger.trace(requestName + " : Non-textual content");
            }
            return reader.read(stream, contentType == null ? null : contentType.mimeType(), contentType == null ? null : contentType.charset());
        });
        logger.debug(requestName + " : Response extracted");
        return result;
    }

    /**
     * Reads the response body with {@code reader} taking care of response gzip encoding
     * <p>
     * If the logging level for {@code logger} is set to {@code TRACE}, logs the textual response body
     */
    public static <T extends @Nullable Object> CompletableFuture<T> readBodyWithLogging(HttpResponse<? extends InputStream> response,
                                                                                        Logger logger,
                                                                                        String requestName,
                                                                                        HttpBodyReader<T> reader) {
        return supplyIO(() -> readBodyWithLoggingBlocking(response, logger, requestName, reader));
    }

    /**
     * Overrides the response body with {@code newBody}
     */
    public static <T extends @Nullable Object> HttpResponse<T> withNewBody(HttpResponse<?> response, T newBody) {
        return new BodyOverrideHttpResponse<>(response, newBody);
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

    private static <T extends @Nullable Object> CompletableFuture<T> supplyIO(Callable<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            if (result.isDone()) {
                return;
            }
            try {
                result.complete(task.call());
            }
            catch (CompletionException e) {
                result.completeExceptionally(e.getCause() == null ? e : e.getCause());
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
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

    private static @Nullable String readBodyAsText(HttpResponse<? extends InputStream> response) throws IOException {
        HttpContentType contentType = getContentType(response);
        if (contentType == null || !isMimeTypeTextual(contentType.mimeType())) {
            return null;
        }
        Charset charset = contentType.charset() == null ? StandardCharsets.UTF_8 : contentType.charset();
        return readBodyInflating(response, stream -> {
            try (Reader reader = new InputStreamReader(stream, charset)) {
                StringWriter writer = new StringWriter();
                reader.transferTo(writer);
                return writer.toString();
            }
        });
    }

    private static @Nullable HttpContentType getContentType(HttpResponse<?> response) {
        Optional<String> header = response.headers().firstValue(CONTENT_TYPE_HEADER);
        if (header.isEmpty()) {
            return null;
        }
        String value = header.get();
        ContentType contentType;
        try {
            contentType = ContentType.parse(value);
        }
        catch (Exception e) {
            LOG.debug("Could not parse " + CONTENT_TYPE_HEADER + " from value " + value, e);
            return null;
        }
        String mimeType = contentType.getMimeType();
        if (mimeType == null) {
            return null;
        }
        return new HttpContentType(mimeType, contentType.getCharset());
    }

    /**
     * Reads the response body with a {@code streamReader} taking care of the gzip encoding
     */
    private static <T extends @Nullable Object> T readBodyInflating(HttpResponse<? extends InputStream> response,
                                                                    HttpStreamReader<T> streamReader) throws IOException {
        Optional<String> encoding = response.headers().firstValue(CONTENT_ENCODING_HEADER);
        boolean isGzipEncoding = false;
        if (encoding.isPresent()) {
            if (!encoding.get().equalsIgnoreCase(CONTENT_ENCODING_GZIP)) {
                throw new IllegalStateException("Unsupported encoding: " + encoding.get());
            }
            isGzipEncoding = true;
        }
        try (InputStream bodyStream = isGzipEncoding ? new GZIPInputStream(response.body()) : response.body()) {
            return streamReader.read(bodyStream);
        }
    }
}
