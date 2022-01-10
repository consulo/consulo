/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.builtInServer.http;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.text.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.annotation.Nullable;
import java.nio.CharBuffer;
import java.util.Calendar;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * from kotlin platform\platform-impl\src\org\jetbrains\io\Responses.kt
 */
public final class Responses {
  private static String SERVER_HEADER_VALUE;

  private static String getServerHeaderValue() {
    if (SERVER_HEADER_VALUE == null) {
      Application app = ApplicationManager.getApplication();
      if (app != null && !app.isDisposed()) {
        SERVER_HEADER_VALUE = ApplicationNamesInfo.getInstance().getFullProductName();
      }
    }
    return SERVER_HEADER_VALUE;
  }

  public static void setDate(HttpResponse response) {
    if (!response.headers().contains(HttpHeaderNames.DATE)) {
      response.headers().set(HttpHeaderNames.DATE, Calendar.getInstance().getTime());
    }
  }

  public static void addServer(HttpResponse response) {
    if (SERVER_HEADER_VALUE == null) {
      Application app = ApplicationManager.getApplication();
      if (app != null && !app.isDisposed()) {
        SERVER_HEADER_VALUE = ApplicationNamesInfo.getInstance().getFullProductName();
      }
    }
    if (SERVER_HEADER_VALUE != null) {
      response.headers().set("Server", SERVER_HEADER_VALUE);
    }
  }

  public static void send(HttpResponseStatus status, Channel channel, HttpRequest request, String description) {
    send(status, channel, request, description, null);
  }

  public static void send(HttpResponseStatus status, Channel channel, String description) {
    send(status, channel, null, description, null);
  }

  public static void send(HttpResponseStatus status, Channel channel, HttpRequest request) {
    send(status, channel, request, null, null);
  }

  public static void send(HttpResponseStatus status,
                          Channel channel,
                          @Nullable HttpRequest request,
                          @Nullable String description,
                          @Nullable HttpHeaders extraHeaders) {
    HttpResponse response = createStatusResponse(status, request, description);
    send(response, channel, request, extraHeaders);
  }

  private static HttpResponse createStatusResponse(HttpResponseStatus responseStatus, HttpRequest request, String description) {
    if (request != null && request.method() == HttpMethod.HEAD) {
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, Unpooled.EMPTY_BUFFER);
    }

    StringBuilder builder = new StringBuilder();
    String message = responseStatus.toString();
    builder.append("<!doctype html><title>").append(message).append("</title>").append("<h1 style=\"text-align: center\">").append(message).append("</h1>");
    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
    }
    builder.append("<hr/><p style=\"text-align: center\">").append(StringUtil.notNullize(getServerHeaderValue())).append("</p>");

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, ByteBufUtil
            .encodeString(ByteBufAllocator.DEFAULT, CharBuffer.wrap(builder), CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    return response;
  }

  public static void send(HttpResponse response, Channel channel, HttpRequest request) {
    send(response, channel, request, null);
  }

  public static void send(HttpResponse response, Channel channel, HttpRequest request, HttpHeaders extraHeaders) {
    if (response.status() != HttpResponseStatus.NOT_MODIFIED && !HttpUtil.isContentLengthSet(response)) {
      long toLong = (response instanceof FullHttpResponse) ? ((FullHttpResponse)response).content().readableBytes() : 0;
      HttpUtil.setContentLength(response, toLong);
    }

    addCommonHeaders(response);

    if (extraHeaders != null) {
      response.headers().add(extraHeaders);
    }

    send(response, channel, request != null && !addKeepAliveIfNeed(response, request));
  }

  static void send(HttpResponse response, Channel channel, boolean close) {
    if (!channel.isActive()) {
      return;
    }

    ChannelFuture future = channel.write(response);
    if (!(response instanceof FullHttpResponse)) {
      channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
    }
    channel.flush();
    if (close) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private static boolean addKeepAliveIfNeed(HttpResponse response, HttpRequest request) {
    if (HttpUtil.isKeepAlive(request)) {
      HttpUtil.setKeepAlive(response, true);
      return true;
    }
    return false;
  }

  public static FullHttpResponse response(String contentType, ByteBuf content) {
    FullHttpResponse response = null;
    if (content == null) {
      response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }
    else {

      response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
    }
    if (contentType != null) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }
    return response;
  }

  public static HttpResponse addNoCache(HttpResponse response) {
    response.headers().add(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0");
    response.headers().add(HttpHeaderNames.PRAGMA, "no-cache");
    return response;
  }

  public static FullHttpResponse create(String contentType) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
    response.headers().set(CONTENT_TYPE, contentType);
    return response;
  }

  private static void addCommonHeaders(HttpResponse response) {
    addServer(response);
    setDate(response);
    if (!response.headers().contains("X-Frame-Options")) {
      response.headers().set("X-Frame-Options", "SameOrigin");
    }
    response.headers().set("x-xss-protection", "1; mode=block");
    response.headers().set("X-Content-Type-Options", "nosniff");
  }
}
