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
package consulo.builtInServer.impl.net.util.netty;

import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Condition;
import consulo.builtInServer.impl.net.http.NettyUtil;
import io.netty.bootstrap.Bootstrap;
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
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
        bootstrap.register().sync();
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
