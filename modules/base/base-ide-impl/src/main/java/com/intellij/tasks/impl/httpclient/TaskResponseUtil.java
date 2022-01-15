/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.tasks.impl.httpclient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.tasks.impl.RequestFailedException;
import com.intellij.tasks.impl.TaskUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

/**
 * @author Mikhail Golubev
 */
public class TaskResponseUtil {
  public static final Logger LOG = Logger.getInstance(TaskResponseUtil.class);

  public static final String DEFAULT_CHARSET_NAME = CharsetToolkit.UTF8;
  public final static Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);

  /**
   * Utility class
   */
  private TaskResponseUtil() {
  }

  public static Reader getResponseContentAsReader(@Nonnull HttpResponse response) throws IOException {
    Header header = response.getEntity().getContentEncoding();
    Charset charset = header == null ? DEFAULT_CHARSET : Charset.forName(header.getValue());
    return new InputStreamReader(response.getEntity().getContent(), charset);
  }

  public static String getResponseContentAsString(@Nonnull HttpResponse response) throws IOException {
    return EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
  }

  public static final class GsonSingleObjectDeserializer<T> implements ResponseHandler<T> {
    private final Gson myGson;
    private final Class<T> myClass;
    private final boolean myIgnoreNotFound;

    public GsonSingleObjectDeserializer(@Nonnull Gson gson, @Nonnull Class<T> cls) {
      this(gson, cls, false);
    }

    public GsonSingleObjectDeserializer(@Nonnull Gson gson, @Nonnull Class<T> cls, boolean ignoreNotFound) {
      myGson = gson;
      myClass = cls;
      myIgnoreNotFound = ignoreNotFound;
    }

    @Override
    public T handleResponse(HttpResponse response) throws IOException {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!isSuccessful(statusCode)) {
        if (statusCode == SC_NOT_FOUND && myIgnoreNotFound) {
          return null;
        }
        throw RequestFailedException.forStatusCode(statusCode);
      }
      try {
        if (LOG.isDebugEnabled()) {
          String content = getResponseContentAsString(response);
          TaskUtil.prettyFormatJsonToLog(LOG, content);
          return myGson.fromJson(content, myClass);
        }
        else {
          return myGson.fromJson(getResponseContentAsReader(response), myClass);
        }
      }
      catch (JsonSyntaxException e) {
        LOG.warn("Malformed server response", e);
        return null;
      }
    }
  }

  public static final class GsonMultipleObjectsDeserializer<T> implements ResponseHandler<List<T>> {
    private final Gson myGson;
    private final TypeToken<List<T>> myTypeToken;
    private final boolean myIgnoreNotFound;

    public GsonMultipleObjectsDeserializer(Gson gson, TypeToken<List<T>> typeToken) {
      this(gson, typeToken, false);
    }

    public GsonMultipleObjectsDeserializer(@Nonnull Gson gson, @Nonnull TypeToken<List<T>> token, boolean ignoreNotFound) {
      myGson = gson;
      myTypeToken = token;
      myIgnoreNotFound = ignoreNotFound;
    }

    @Override
    public List<T> handleResponse(HttpResponse response) throws IOException {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!isSuccessful(statusCode)) {
        if (statusCode == SC_NOT_FOUND && myIgnoreNotFound) {
          return Collections.emptyList();
        }
        throw RequestFailedException.forStatusCode(statusCode);
      }
      try {
        if (LOG.isDebugEnabled()) {
          String content = getResponseContentAsString(response);
          TaskUtil.prettyFormatJsonToLog(LOG, content);
          return myGson.fromJson(content, myTypeToken.getType());
        }
        else {
          return myGson.fromJson(getResponseContentAsReader(response), myTypeToken.getType());
        }
      }
      catch (JsonSyntaxException e) {
        LOG.warn("Malformed server response", e);
        return Collections.emptyList();
      }
      catch (NumberFormatException e) {
        LOG.error("NFE in response: " + getResponseContentAsString(response), e);
        throw new RequestFailedException("Malformed response");
      }
    }
  }

  public static void prettyFormatResponseToLog(@Nonnull Logger logger, @Nonnull HttpResponse response) {
    if (logger.isDebugEnabled()) {
      try {
        String content = TaskResponseUtil.getResponseContentAsString(response);
        Header header = response.getEntity().getContentType();
        String contentType = header == null ? "text/plain" : header.getElements()[0].getName().toLowerCase(Locale.ENGLISH);
        if (contentType.contains("xml")) {
          TaskUtil.prettyFormatXmlToLog(logger, content);
        }
        else if (contentType.contains("json")) {
          TaskUtil.prettyFormatJsonToLog(logger, content);
        }
        else {
          logger.debug(content);
        }
      }
      catch (IOException e) {
        logger.error(e);
      }
    }
  }

  public static boolean isSuccessful(int statusCode) {
    return statusCode / 100 == 2;
  }

  public static boolean isClientError(int statusCode) {
    return statusCode / 100 == 4;
  }

  public static boolean isServerError(int statusCode) {
    return statusCode / 100 == 5;
  }
}
