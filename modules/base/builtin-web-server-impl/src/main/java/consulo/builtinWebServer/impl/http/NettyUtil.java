/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.builtinWebServer.impl.http;

import consulo.logging.Logger;
import consulo.util.netty.NettyKt;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.TestOnly;
import consulo.application.util.concurrent.PooledThreadExecutor;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

public final class NettyUtil {
  public static final int MAX_CONTENT_LENGTH = NettyKt.MAX_CONTENT_LENGTH;

  public static final int DEFAULT_CONNECT_ATTEMPT_COUNT = NettyKt.DEFAULT_CONNECT_ATTEMPT_COUNT;
  public static final int MIN_START_TIME = NettyKt.MIN_START_TIME;

  public static void logAndClose(@Nonnull Throwable error, @Nonnull Logger log, @Nonnull Channel channel) {
    // don't report about errors while connecting
    // WEB-7727
    try {
      if (error instanceof ConnectException) {
        log.debug(error);
      }
      else {
        log(error, log);
      }
    }
    finally {
      log.info("Channel will be closed due to error");
      channel.close();
    }
  }

  public static void log(@Nonnull Throwable throwable, @Nonnull Logger log) {
    if (isAsWarning(throwable)) {
      log.warn(throwable);
    }
    else {
      log.error(throwable);
    }
  }

  private static boolean isAsWarning(@Nonnull Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null) {
      return false;
    }

    return (throwable instanceof IOException && message.equals("An existing connection was forcibly closed by the remote host")) ||
           (throwable instanceof ChannelException && message.startsWith("Failed to bind to: ")) ||
           throwable instanceof BindException ||
           (message.startsWith("Connection reset") || message.equals("Operation timed out") || message.equals("Connection timed out"));
  }

  public static Bootstrap nioClientBootstrap() {
    return nioClientBootstrap(new NioEventLoopGroup(1, PooledThreadExecutor.getInstance()));
  }

  public static Bootstrap nioClientBootstrap(@Nonnull EventLoopGroup eventLoopGroup) {
    Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class);
    bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);
    return bootstrap;
  }

  public static void addHttpServerCodec(@Nonnull ChannelPipeline pipeline) {
    pipeline.addLast("httpRequestEncoder", new HttpResponseEncoder());
    // https://jetbrains.zendesk.com/agent/tickets/68315
    pipeline.addLast("httpRequestDecoder", new HttpRequestDecoder(16 * 1024, 16 * 1024, 8192));
    pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    // could be added earlier if HTTPS
    if (pipeline.get(ChunkedWriteHandler.class) == null) {
      pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
    }
    pipeline.addLast("corsHandler", new CorsHandlerDoNotUseOwnLogger(CorsConfigBuilder
                                                                             .forAnyOrigin()
                                                                             .shortCircuit()
                                                                             .allowCredentials()
                                                                             .allowNullOrigin()
                                                                             .allowedRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.PATCH)
                                                                             .allowedRequestHeaders("origin", "accept", "authorization", "content-type", "x-ijt")
                                                                             .build()));
  }

  private static final class CorsHandlerDoNotUseOwnLogger extends CorsHandler {
    public CorsHandlerDoNotUseOwnLogger(@Nonnull CorsConfig config) {
      super(config);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
      context.fireExceptionCaught(cause);
    }
  }

  @TestOnly
  public static void awaitQuiescenceOfGlobalEventExecutor(long timeout, @Nonnull TimeUnit unit) {
    try {
      @Nonnull GlobalEventExecutor executor = GlobalEventExecutor.INSTANCE;
      executor.awaitInactivity(timeout, unit);
    }
    catch (InterruptedException ignored) {

    }
    catch (IllegalStateException ignored) {
      // thread did not start
    }
  }
}
