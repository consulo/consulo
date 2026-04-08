// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import com.intellij.collaboration.api.HttpApiHelper;
import com.intellij.collaboration.api.HttpStatusErrorException;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.SystemInfo;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class HttpClientUtil {
    public static final @Nonnull String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    public static final @Nonnull String CONTENT_ENCODING_HEADER = "Content-Encoding";
    public static final @Nonnull String CONTENT_ENCODING_GZIP = "gzip";

    public static final @Nonnull String CONTENT_TYPE_HEADER = "Content-Type";
    public static final @Nonnull String CONTENT_TYPE_JSON = "application/json";

    public static final @Nonnull String USER_AGENT_HEADER = "User-Agent";

    private HttpClientUtil() {
    }

    /**
     * Checks the status code of the response and throws {@link HttpStatusErrorException} if status code is not a successful one.
     * <p>
     * Logs request status code and also response body if tracing is enabled in logger.
     */
    public static void checkStatusCodeWithLogging(
        @Nonnull Logger logger,
        @Nonnull String requestName,
        int statusCode,
        @Nonnull InputStream bodyStream
    ) {
        logger.debug(requestName + " : Status code " + statusCode);
        if (statusCode >= 400) {
            String errorBody;
            try {
                errorBody = new String(bodyStream.readAllBytes());
            }
            catch (Exception e) {
                errorBody = "<failed to read error body>";
            }
            if (logger.isTraceEnabled()) {
                logger.trace(requestName + " : Response body: " + errorBody);
            }
            throw new HttpStatusErrorException(requestName, statusCode, errorBody);
        }
    }

    /**
     * Reads the response from input stream, logging the response if tracing is enabled in logger.
     * <p>
     * It is usually better to read the response directly from stream to avoid creating too many strings,
     * but when tracing is enabled we need to read the response to string first to log it.
     */
    private static @Nonnull Reader responseReaderWithLogging(
        @Nonnull Logger logger, @Nonnull String requestName,
        @Nonnull InputStream stream
    ) {
        if (logger.isTraceEnabled()) {
            String body;
            try (var reader = new InputStreamReader(stream)) {
                body = new String(stream.readAllBytes());
            }
            catch (Exception e) {
                body = "<failed to read body>";
            }
            logger.trace(requestName + " : Response body: " + body);
            return new StringReader(body);
        }
        return new InputStreamReader(stream);
    }

    /**
     * Reads the request response if the request completed successfully, otherwise throws {@link HttpStatusErrorException}.
     * Response status is always logged, response body is logged when tracing is enabled in logger.
     */
    public static <T> T readSuccessResponseWithLogging(
        @Nonnull Logger logger,
        @Nonnull HttpRequest request,
        HttpResponse.@Nonnull ResponseInfo responseInfo,
        @Nonnull InputStream bodyStream,
        @Nonnull Function<Reader, T> reader
    ) {
        String logName = HttpApiHelper.logName(request);
        checkStatusCodeWithLogging(logger, logName, responseInfo.statusCode(), bodyStream);
        try (Reader responseReader = responseReaderWithLogging(logger, logName, bodyStream)) {
            return reader.apply(responseReader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Shorthand for creating a body handler that inflates the incoming response body if it is zipped, checks that
     * the status code is OK (throws {@link HttpStatusErrorException} otherwise), and applies the given function to read
     * the result body and map it to some value.
     *
     * @param logger      The logger to log non-OK status codes in.
     * @param request     The request performed, for logging purposes.
     * @param mapToResult Maps a response to a result value. Exceptions thrown from this function are not logged by
     *                    {@link #inflateAndReadWithErrorHandlingAndLogging}.
     */
    public static <T> HttpResponse.@Nonnull BodyHandler<T> inflateAndReadWithErrorHandlingAndLogging(
        @Nonnull Logger logger,
        @Nonnull HttpRequest request,
        @Nonnull BiFunction<Reader, HttpResponse.ResponseInfo, T> mapToResult
    ) {
        return new InflatedStreamReadingBodyHandler<>(
            (responseInfo, bodyStream) -> readSuccessResponseWithLogging(
                logger,
                request,
                responseInfo,
                bodyStream,
                reader -> mapToResult.apply(reader, responseInfo)
            )
        );
    }

    /**
     * Build the User-Agent header value for the given agent name.
     * Appends product, java and OS data.
     */
    public static @Nonnull String getUserAgentValue(@Nonnull String agentName) {
        String ideName = ApplicationNamesInfo.getInstance().getFullProductName().replace(' ', '-');
        String ideBuild;
        if (Application.get().isUnitTestMode()) {
            ideBuild = "test";
        }
        else {
            ideBuild = ApplicationInfo.getInstance().getBuild().asStringWithoutProductCode();
        }
        String java = "JRE " + SystemInfo.JAVA_RUNTIME_VERSION;
        String os = SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION;
        String arch = SystemInfo.OS_ARCH;

        return agentName + " " + ideName + "/" + ideBuild + " (" + java + "; " + os + "; " + arch + ")";
    }
}
