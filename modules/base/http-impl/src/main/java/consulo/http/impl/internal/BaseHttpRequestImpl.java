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

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.ProgressStreamUtil;
import consulo.http.HttpRequest;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.CountingGZIPInputStream;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reading of an answer, the same for every {@link HttpRequestExecutor}: the body stream, gzip, the charset, the
 * progress and the saving to a file. The executor gives the raw body and its length.
 *
 * @author VISTALL
 * @since 2026-02-18
 */
public abstract class BaseHttpRequestImpl implements HttpRequest, AutoCloseable {
    static final int BLOCK_SIZE = 16 * 1024;

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([^;]+)");

    protected final HttpRequestOptions myOptions;

    private @Nullable InputStream myInputStream;
    private @Nullable BufferedReader myReader;

    protected BaseHttpRequestImpl(HttpRequestOptions options) {
        myOptions = options;
    }

    /**
     * The body as it came, connecting if not yet.
     */
    protected abstract InputStream openInputStream() throws IOException;

    /**
     * The length of the body as it came, or -1 if not known.
     */
    protected abstract int getContentLength() throws IOException;

    /**
     * Releases the connection - the streams are closed already.
     */
    protected abstract void disconnect();

    @Override
    public InputStream getInputStream() throws IOException {
        if (myInputStream == null) {
            myInputStream = openInputStream();
            if (myOptions.gzip() && "gzip".equalsIgnoreCase(getContentEncoding())) {
                myInputStream = CountingGZIPInputStream.create(myInputStream);
            }
        }
        return myInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return getReader(null);
    }

    @Override
    public BufferedReader getReader(@Nullable ProgressIndicator indicator) throws IOException {
        if (myReader == null) {
            InputStream inputStream = getInputStream();
            if (indicator != null) {
                int contentLength = getContentLength();
                if (contentLength > 0) {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    inputStream = new ProgressMonitorInputStream(indicator, inputStream, contentLength);
                }
            }
            myReader = new BufferedReader(new InputStreamReader(inputStream, getCharset()));
        }
        return myReader;
    }

    @Override
    public byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException {
        int contentLength = getContentLength();
        BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream(contentLength > 0 ? contentLength : BLOCK_SIZE);
        ProgressStreamUtil.copyStreamContent(indicator, getInputStream(), out, contentLength);
        return ArrayUtil.realloc(out.getInternalBuffer(), out.size());
    }

    @Override
    public String readString(@Nullable ProgressIndicator indicator) throws IOException {
        Charset cs = getCharset();
        byte[] bytes = readBytes(indicator);
        return new String(bytes, cs);
    }

    @Override
    public Path saveToFile(Path file, @Nullable MessageDigest digest, @Nullable ProgressIndicator indicator) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean deleteFile = true;
        try {
            try (OutputStream out = Files.newOutputStream(file)) {
                ProgressStreamUtil.copyStreamContent(indicator, digest, getInputStream(), out, getContentLength());
                deleteFile = false;
            }
            catch (IOException e) {
                throw new IOException(createErrorMessage(e, false), e);
            }
        }
        finally {
            if (deleteFile) {
                Files.deleteIfExists(file);
            }
        }

        return file;
    }

    protected Charset getCharset() throws IOException {
        String contentType = getContentType();
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

    protected String createErrorMessage(IOException e, boolean includeHeaders) {
        StringBuilder builder = new StringBuilder();

        builder.append("Cannot download '").append(getURL()).append("': ").append(e.getMessage());

        try {
            if (includeHeaders) {
                builder.append("\n, headers: ").append(responseHeaders());
            }

            int statusCode = statusCode();
            // zero is not a http connection
            if (statusCode != 0) {
                builder.append("\n, response: ").append(statusCode).append(' ').append(statusMessage());
            }
        }
        catch (Throwable ignored) {
        }

        return builder.toString();
    }

    @Override
    public void close() {
        StreamUtil.closeStream(myInputStream);
        StreamUtil.closeStream(myReader);
        disconnect();
    }
}
