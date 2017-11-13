/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.util.io;

import com.google.common.net.InetAddresses;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableNotNullFunction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.net.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.BootstrapUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.ResolvedAddressTypes;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;
import org.jetbrains.io.NettyUtil;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform/platform-impl/src/com/intellij/util/io/netty.kt
 */
public class NettyKt {
  public static Bootstrap oioClientBootstrap() {
    Bootstrap bootstrap = new Bootstrap().group(new OioEventLoopGroup(1, PooledThreadExecutor.INSTANCE)).channel(OioSocketChannel.class);
    bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);
    return bootstrap;
  }

  public static ServerBootstrap serverBootstrap(EventLoopGroup group) {
    ServerBootstrap bootstrap =
            new ServerBootstrap().group(group).channel(group instanceof NioEventLoopGroup ? NioServerSocketChannel.class : OioServerSocketChannel.class);
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true);
    return bootstrap;
  }

  public static String getHost(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.HOST);
  }

  public static String getOrigin(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.ORIGIN);
  }

  public static String getReferrer(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.REFERER);
  }

  public static String getUserAgent(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.USER_AGENT);
  }

  // forbid POST requests from browser without Origin
  public static boolean isWriteFromBrowserWithoutOrigin(HttpRequest request) {
    HttpMethod method = request.method();

    return StringUtil.isEmpty(getOrigin(request)) &&
           isRegularBrowser(request) &&
           (method == HttpMethod.POST || method == HttpMethod.PATCH || method == HttpMethod.PUT || method == HttpMethod.DELETE);
  }

  public static boolean isRegularBrowser(HttpRequest request) {
    String userAgent = getUserAgent(request);
    return userAgent != null && StringUtil.startsWith(userAgent, "Mozilla/5.0");
  }

  private static String getHost(Url uri) {
    String authority = uri.getAuthority();
    if (authority != null) {
      int portIndex = authority.indexOf(':');
      if (portIndex > 0) {
        return authority.substring(0, portIndex);
      }
      else {
        return authority;
      }
    }
    return null;
  }

  public static boolean parseAndCheckIsLocalHost(String uri) {
    return parseAndCheckIsLocalHost(uri, true, false);
  }

  public static boolean parseAndCheckIsLocalHost(String uri, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    if (uri == null || uri.equals("about:blank")) {
      return true;
    }

    try {
      Url parsedUri = Urls.parse(uri, false);
      if (parsedUri == null) {
        return false;
      }

      String host = getHost(parsedUri);

      return host != null && (isTrustedChromeExtension(parsedUri) || isLocalHost(host, onlyAnyOrLoopback, hostsOnly));
    }
    catch (Exception ignored) {
    }
    return false;
  }

  public static boolean isLocalOrigin(HttpRequest httpRequest) {
    return isLocalOrigin(httpRequest, true, false);
  }

  public static boolean isLocalOrigin(HttpRequest httpRequest, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    return parseAndCheckIsLocalHost(getOrigin(httpRequest), onlyAnyOrLoopback, hostsOnly) &&
           parseAndCheckIsLocalHost(getReferrer(httpRequest), onlyAnyOrLoopback, hostsOnly);
  }

  public static boolean isLocalHost(String host, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    if (NetUtils.isLocalhost(host)) {
      return true;
    }

    // if IP address, it is safe to use getByName (not affected by DNS rebinding)
    if (onlyAnyOrLoopback && !InetAddresses.isInetAddress(host)) {
      return false;
    }

    ThrowableNotNullFunction<InetAddress, Boolean, SocketException> isLocal =
            inetAddress -> inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || NetworkInterface.getByInetAddress(inetAddress) != null;

    try {
      InetAddress address = InetAddress.getByName(host);
      if (!isLocal.fun(address)) {
        return false;
      }
      // be aware - on windows hosts file doesn't contain localhost
      // hosts can contain remote addresses, so, we check it
      if (hostsOnly && !InetAddresses.isInetAddress(host)) {
        InetAddress hostInetAddress = HostsFileEntriesResolver.DEFAULT.address(host, ResolvedAddressTypes.IPV4_PREFERRED);
        return hostInetAddress != null && isLocal.fun(hostInetAddress);
      }
      else {
        return true;
      }
    }
    catch (IOException ignored) {
      return false;
    }
  }

  private static boolean isTrustedChromeExtension(Url url) {
    /*  FIXME [VISTALL] this is only jetbrains plugins
    return Comparing.equal(url.getScheme(), "chrome-extension") &&  (Comparing.equal(url.getAuthority(), "hmhgeddbohgjknpmjagkdomcpobmllji") || Comparing
            .equal(url.getAuthority(), "offnedcbhjldheanlbojaefbfbllddna"));
            */
    return false;
  }

  public static String readUtf8(ByteBuf buf) {
    return buf.toString(StandardCharsets.UTF_8);
  }

  public static int writeUtf8(ByteBuf buf, CharSequence data) {
    return buf.writeCharSequence(data, StandardCharsets.UTF_8);
  }

  public static Channel connect(Bootstrap bootstrap,
                                InetSocketAddress remoteAddress,
                                AsyncResult<?> promise,
                                int maxAttemptCount,
                                Condition<Void> stopCondition) {
    try {
      return doConnect(bootstrap, remoteAddress, promise, maxAttemptCount, stopCondition);
    }
    catch (Throwable e) {
      if (promise != null) {
        promise.rejectWithThrowable(e);
      }
      return null;
    }
  }

  private static Channel doConnect(Bootstrap bootstrap,
                                   InetSocketAddress remoteAddress,
                                   AsyncResult<?> asyncResult,
                                   int maxAttemptCount,
                                   @Nullable Condition<Void> stopCondition) throws Throwable {
    int attemptCount = 0;
    if (bootstrap.config().group() instanceof NioEventLoopGroup) {
      return connectNio(bootstrap, remoteAddress, asyncResult, maxAttemptCount, stopCondition, attemptCount);
    }

    bootstrap.validate();

    while (true) {
      try {
        OioSocketChannel channel = new OioSocketChannel(new Socket(remoteAddress.getAddress(), remoteAddress.getPort()));
        BootstrapUtil.initAndRegister(channel, bootstrap).sync();
        return channel;
      }
      catch (IOException e) {
        if (stopCondition != null && stopCondition.value(null) || asyncResult != null && !asyncResult.isProcessed()) {
          return null;
        }
        else if (maxAttemptCount == -1) {
          if (sleep(asyncResult, 300)) {
            return null;
          }
          attemptCount++;
        }
        else if (++attemptCount < maxAttemptCount) {
          if (sleep(asyncResult, attemptCount * NettyUtil.MIN_START_TIME)) {
            return null;
          }
        }
        else {
          if (asyncResult != null) {
            asyncResult.rejectWithThrowable(e);
          }
          return null;
        }
      }
    }
  }

  private static Channel connectNio(Bootstrap bootstrap,
                                    InetSocketAddress remoteAddress,
                                    AsyncResult<?> promise,
                                    int maxAttemptCount,
                                    @Nullable Condition<Void> stopCondition,
                                    int _attemptCount) {
    int attemptCount = _attemptCount;
    while (true) {
      ChannelFuture future = bootstrap.connect(remoteAddress).awaitUninterruptibly();
      if (future.isSuccess()) {
        if (!future.channel().isOpen()) {
          continue;
        }
        return future.channel();
      }
      else if (stopCondition != null && stopCondition.value(null) || promise != null && promise.isRejected()) {
        return null;
      }
      else if (maxAttemptCount == -1) {
        if (sleep(promise, 300)) {
          return null;
        }
        attemptCount++;
      }
      else if (++attemptCount < maxAttemptCount) {
        if (sleep(promise, attemptCount * NettyUtil.MIN_START_TIME)) {
          return null;
        }
      }
      else {
        Throwable cause = future.cause();
        if (promise != null) {
          if (cause == null) {
            promise.reject("Cannot connect: unknown error");
          }
          else {
            promise.rejectWithThrowable(cause);
          }
        }
        return null;
      }
    }
  }


  public static boolean sleep(AsyncResult<?> promise, int time) {
    try {
      //noinspection BusyWait
      Thread.sleep(time);
    }
    catch (InterruptedException ignored) {
      if (promise != null) {
        promise.reject("Interrupted");
      }
      return true;
    }

    return false;
  }
}
