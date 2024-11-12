/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.http;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.ProgressStreamUtil;
import consulo.http.internal.ProgressMonitorInputStream;
import consulo.http.localize.HttpLocalize;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.CountingGZIPInputStream;
import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handy class for reading data from HTTP connections with built-in support for HTTP redirects and gzipped content and automatic cleanup.
 * Usage: <pre>{@code
 * int firstByte = HttpRequests.request(url).connect(new HttpRequests.RequestProcessor<Integer>() {
 *   public Integer process(@NotNull Request request) throws IOException {
 *     return request.getInputStream().read();
 *   }
 * });
 * }</pre>
 */
public final class HttpRequests {
  private static final Logger LOG = Logger.getInstance(HttpRequests.class);

  private static final int BLOCK_SIZE = 16 * 1024;
  private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([^;]+)");

  private HttpRequests() {
  }


  public interface Request {
    @Nonnull
    String getURL();

    @Nonnull
    URLConnection getConnection() throws IOException;

    @Nonnull
    InputStream getInputStream() throws IOException;

    @Nonnull
    BufferedReader getReader() throws IOException;

    @Nonnull
    BufferedReader getReader(@Nullable ProgressIndicator indicator) throws IOException;

    /**
     * @deprecated Called automatically on open connection. Use {@link RequestBuilder#tryConnect()} to get response code
     */
    boolean isSuccessful() throws IOException;

    @Nonnull
    default File saveToFile(@Nonnull File file, @Nullable ProgressIndicator indicator) throws IOException {
      return saveToFile(file, null, indicator);
    }

    @Nonnull
    File saveToFile(@Nonnull File file, @Nullable MessageDigest digest, @Nullable ProgressIndicator indicator) throws IOException;

    @Nonnull
    byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException;

    @Nonnull
    String readString(@Nullable ProgressIndicator indicator) throws IOException;
  }

  public interface RequestProcessor<T> {
    T process(@Nonnull Request request) throws IOException;
  }

  public interface ConnectionTuner {
    void tune(@Nonnull URLConnection connection) throws IOException;
  }

  public static class HttpStatusException extends IOException {
    private int myStatusCode;
    private String myUrl;

    public HttpStatusException(@Nonnull String message, int statusCode, @Nonnull String url) {
      super(message);
      myStatusCode = statusCode;
      myUrl = url;
    }

    public int getStatusCode() {
      return myStatusCode;
    }

    @Nonnull
    public String getUrl() {
      return myUrl;
    }

    @Override
    public String getMessage() {
      return "Status: " + myStatusCode;
    }

    @Override
    public String toString() {
      return super.toString() + ". Status=" + myStatusCode + ", Url=" + myUrl;
    }
  }


  @Nonnull
  public static RequestBuilder request(@Nonnull String url) {
    return new RequestBuilderImpl(url);
  }

  @Nonnull
  public static String createErrorMessage(@Nonnull IOException e, @Nonnull Request request, boolean includeHeaders) {
    StringBuilder builder = new StringBuilder();

    builder.append("Cannot download '").append(request.getURL()).append("': ").append(e.getMessage());

    try {
      URLConnection connection = request.getConnection();
      if (includeHeaders) {
        builder.append("\n, headers: ").append(connection.getHeaderFields());
      }
      if (connection instanceof HttpURLConnection) {
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
        builder.append("\n, response: ").append(httpConnection.getResponseCode()).append(' ').append(httpConnection.getResponseMessage());
      }
    }
    catch (Throwable ignored) {
    }

    return builder.toString();
  }


  private static class RequestBuilderImpl extends RequestBuilder {
    private final String myUrl;
    private int myConnectTimeout = HttpProxyManager.CONNECTION_TIMEOUT;
    private int myTimeout = HttpProxyManager.READ_TIMEOUT;
    private int myRedirectLimit = HttpProxyManager.REDIRECT_LIMIT;
    private boolean myGzip = true;
    private boolean myForceHttps;
    private boolean myUseProxy = true;
    private HostnameVerifier myHostnameVerifier;
    private String myUserAgent;
    private String myAccept;
    private ConnectionTuner myTuner;

    private RequestBuilderImpl(@Nonnull String url) {
      myUrl = url;
    }

    @Override
    public RequestBuilder connectTimeout(int value) {
      myConnectTimeout = value;
      return this;
    }

    @Override
    public RequestBuilder readTimeout(int value) {
      myTimeout = value;
      return this;
    }

    @Override
    public RequestBuilder redirectLimit(int redirectLimit) {
      myRedirectLimit = redirectLimit;
      return this;
    }

    @Override
    public RequestBuilder gzip(boolean value) {
      myGzip = value;
      return this;
    }

    @Override
    public RequestBuilder forceHttps(boolean forceHttps) {
      myForceHttps = forceHttps;
      return this;
    }

    @Override
    public RequestBuilder useProxy(boolean useProxy) {
      myUseProxy = useProxy;
      return this;
    }

    @Override
    public RequestBuilder hostNameVerifier(@Nullable HostnameVerifier hostnameVerifier) {
      myHostnameVerifier = hostnameVerifier;
      return this;
    }

    @Override
    public RequestBuilder userAgent(@Nullable String userAgent) {
      myUserAgent = userAgent;
      return this;
    }

    @Override
    public RequestBuilder productNameAsUserAgent() {
      Application app = ApplicationManager.getApplication();
      if (app != null && !app.isDisposed()) {
        ApplicationInfo info = ApplicationInfo.getInstance();
        return userAgent(info.getVersionName() + '/' + info.getBuild().asString());
      }
      else {
        return userAgent("Consulo");
      }
    }

    @Override
    public RequestBuilder accept(@Nullable String mimeType) {
      myAccept = mimeType;
      return this;
    }

    @Override
    public RequestBuilder tuner(@Nullable ConnectionTuner tuner) {
      myTuner = tuner;
      return this;
    }

    @Override
    public <T> T connect(@Nonnull HttpRequests.RequestProcessor<T> processor) throws IOException {
      return process(this, processor);
    }
  }

  private static class RequestImpl implements Request, AutoCloseable {
    private final RequestBuilderImpl myBuilder;
    private URLConnection myConnection;
    private InputStream myInputStream;
    private BufferedReader myReader;

    private RequestImpl(RequestBuilderImpl builder) {
      myBuilder = builder;
    }

    @Nonnull
    @Override
    public String getURL() {
      return myBuilder.myUrl;
    }

    @Nonnull
    @Override
    public URLConnection getConnection() throws IOException {
      if (myConnection == null) {
        myConnection = openConnection(myBuilder);
      }
      return myConnection;
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
        myReader = new BufferedReader(new InputStreamReader(inputStream, getCharset(this)));
      }
      return myReader;
    }

    @Override
    public boolean isSuccessful() throws IOException {
      URLConnection connection = getConnection();
      return !(connection instanceof HttpURLConnection) || ((HttpURLConnection)connection).getResponseCode() == 200;
    }

    @Override
    @Nonnull
    public byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException {
      int contentLength = getConnection().getContentLength();
      BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream(contentLength > 0 ? contentLength : BLOCK_SIZE);
      ProgressStreamUtil.copyStreamContent(indicator, getInputStream(), out, contentLength);
      return ArrayUtil.realloc(out.getInternalBuffer(), out.size());
    }

    @Nonnull
    @Override
    public String readString(@Nullable ProgressIndicator indicator) throws IOException {
      Charset cs = getCharset(this);
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
          throw new IOException(createErrorMessage(e, this, false), e);
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
        ((HttpURLConnection)myConnection).disconnect();
      }
    }
  }

  private static <T> T process(RequestBuilderImpl builder, RequestProcessor<T> processor) throws IOException {
    LOG.assertTrue(ApplicationManager.getApplication() == null || !ApplicationManager.getApplication().isReadAccessAllowed(), "Network shouldn't be accessed in EDT or inside read action");

    return doProcess(builder, processor);
  }

  private static <T> T doProcess(RequestBuilderImpl builder, RequestProcessor<T> processor) throws IOException {
    try (RequestImpl request = new RequestImpl(builder)) {
      return processor.process(request);
    }
  }

  private static Charset getCharset(Request request) throws IOException {
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

  private static URLConnection openConnection(RequestBuilderImpl builder) throws IOException {
    String url = builder.myUrl;

    for (int i = 0; i < builder.myRedirectLimit; i++) {
      if (builder.myForceHttps && StringUtil.startsWith(url, "http:")) {
        url = "https:" + url.substring(5);
      }

      URLConnection connection;
      if (!builder.myUseProxy) {
        connection = new URL(url).openConnection(Proxy.NO_PROXY);
      }
      else if (ApplicationManager.getApplication() == null) {
        connection = new URL(url).openConnection();
      }
      else {
        connection = HttpProxyManager.getInstance().openConnection(url);
      }

      if (connection instanceof HttpURLConnection httpURLConnection) {
        // we will control redirection by code lower
        httpURLConnection.setInstanceFollowRedirects(false);
      }

      if (connection instanceof HttpsURLConnection httpsURLConnection) {
        httpsURLConnection.setSSLSocketFactory(CertificateManager.getInstance().getSslContext().getSocketFactory());
      }

      connection.setConnectTimeout(builder.myConnectTimeout);
      connection.setReadTimeout(builder.myTimeout);

      if (builder.myUserAgent != null) {
        connection.setRequestProperty("User-Agent", builder.myUserAgent);
      }

      if (builder.myHostnameVerifier != null && connection instanceof HttpsURLConnection) {
        ((HttpsURLConnection)connection).setHostnameVerifier(builder.myHostnameVerifier);
      }

      if (builder.myGzip) {
        connection.setRequestProperty("Accept-Encoding", "gzip");
      }

      if (builder.myAccept != null) {
        connection.setRequestProperty("Accept", builder.myAccept);
      }

      connection.setUseCaches(false);

      if (builder.myTuner != null) {
        builder.myTuner.tune(connection);
      }

      if (connection instanceof HttpURLConnection) {
        int responseCode = ((HttpURLConnection)connection).getResponseCode();

        if (responseCode < 200 || responseCode >= 300 && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
          ((HttpURLConnection)connection).disconnect();

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
}