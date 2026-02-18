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
import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
* @author VISTALL
* @since 2026-02-18
*/
class HttpRequestImpl implements HttpRequest, AutoCloseable {
    private final HttpRequestBuilderImpl myBuilder;
    private URLConnection myConnection;
    private InputStream myInputStream;
    private BufferedReader myReader;

    HttpRequestImpl(HttpRequestBuilderImpl builder) {
        myBuilder = builder;
    }

    @Nonnull
    @Override
    public String getURL() {
        return myBuilder.myUrl;
    }

    @Nonnull
    public URLConnection getConnection() throws IOException {
        if (myConnection == null) {
            myConnection = HttpRequestBuilderFactoryImpl.openConnection(myBuilder);
        }
        return myConnection;
    }

    @Nullable
    @Override
    public String getContentEncoding() throws IOException {
        return getConnection().getContentEncoding();
    }

    @Nonnull
    @Override
    public InputStream getInputStream() throws IOException {
        if (myInputStream == null) {
            myInputStream = getConnection().getInputStream();
            if (myBuilder.myGzip && "gzip".equalsIgnoreCase(getConnection().getContentEncoding())) {
                myInputStream = CountingGZIPInputStream.create(myInputStream);
            }
        }
        return myInputStream;
    }

    @Nonnull
    @Override
    public BufferedReader getReader() throws IOException {
        return getReader(null);
    }

    @Nonnull
    @Override
    public BufferedReader getReader(@Nullable ProgressIndicator indicator) throws IOException {
        if (myReader == null) {
            InputStream inputStream = getInputStream();
            if (indicator != null) {
                int contentLength = getConnection().getContentLength();
                if (contentLength > 0) {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    inputStream = new ProgressMonitorInputStream(indicator, inputStream, contentLength);
                }
            }
            myReader = new BufferedReader(new InputStreamReader(inputStream, HttpRequestBuilderFactoryImpl.getCharset(this)));
        }
        return myReader;
    }

    @Override
    public boolean isSuccessful() throws IOException {
        URLConnection connection = getConnection();
        return !(connection instanceof HttpURLConnection) || ((HttpURLConnection) connection).getResponseCode() == 200;
    }

    @Override
    @Nonnull
    public byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException {
        int contentLength = getConnection().getContentLength();
        BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream(contentLength > 0 ? contentLength : HttpRequestBuilderFactoryImpl.BLOCK_SIZE);
        ProgressStreamUtil.copyStreamContent(indicator, getInputStream(), out, contentLength);
        return ArrayUtil.realloc(out.getInternalBuffer(), out.size());
    }

    @Nonnull
    @Override
    public String readString(@Nullable ProgressIndicator indicator) throws IOException {
        Charset cs = HttpRequestBuilderFactoryImpl.getCharset(this);
        byte[] bytes = readBytes(indicator);
        return new String(bytes, cs);
    }

    @Override
    @Nonnull
    public File saveToFile(@Nonnull File file, @Nullable MessageDigest digest, @Nullable ProgressIndicator indicator) throws IOException {
        FileUtil.createParentDirs(file);

        boolean deleteFile = true;
        try {
            try (OutputStream out = new FileOutputStream(file)) {
                ProgressStreamUtil.copyStreamContent(indicator, digest, getInputStream(), out, getConnection().getContentLength());
                deleteFile = false;
            }
            catch (IOException e) {
                throw new IOException(HttpRequestBuilderFactoryImpl.createErrorMessage(e, this, false), e);
            }
        }
        finally {
            if (deleteFile) {
                FileUtil.delete(file);
            }
        }

        return file;
    }

    @Override
    public void close() {
        StreamUtil.closeStream(myInputStream);
        StreamUtil.closeStream(myReader);
        if (myConnection instanceof HttpURLConnection) {
            ((HttpURLConnection) myConnection).disconnect();
        }
    }
}
