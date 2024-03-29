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
package consulo.builtinWebServer.impl.http;

import consulo.application.Application;
import consulo.application.util.AtomicNotNullLazyValue;
import consulo.builtinWebServer.impl.BinaryRequestHandler;
import consulo.logging.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import jakarta.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyStore;
import java.security.Security;
import java.util.UUID;

@ChannelHandler.Sharable
class PortUnificationServerHandler extends Decoder {
  // keytool -genkey -keyalg RSA -alias selfsigned -keystore cert.jks -storepass jetbrains -validity 10000 -keysize 2048
  @SuppressWarnings("SpellCheckingInspection")
  private static final AtomicNotNullLazyValue<SSLContext> SSL_SERVER_CONTEXT = new AtomicNotNullLazyValue<SSLContext>() {
    @Nonnull
    @Override
    protected SSLContext compute() {
      String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
      if (algorithm == null) {
        algorithm = "SunX509";
      }

      try {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] password = "jetbrains".toCharArray();
        //noinspection IOResourceOpenedButNotSafelyClosed
        ks.load(getClass().getResourceAsStream("cert.jks"), password);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, password);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf.getKeyManagers(), null, null);
        return serverContext;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  };

  private final boolean detectSsl;
  private final boolean detectGzip;

  private final DelegatingHttpRequestHandler delegatingHttpRequestHandler;

  public PortUnificationServerHandler() {
    this(new DelegatingHttpRequestHandler(), true, true);
  }

  private PortUnificationServerHandler(@Nonnull DelegatingHttpRequestHandler delegatingHttpRequestHandler, boolean detectSsl, boolean detectGzip) {
    this.delegatingHttpRequestHandler = delegatingHttpRequestHandler;
    this.detectSsl = detectSsl;
    this.detectGzip = detectGzip;
  }

  @Override
  protected void messageReceived(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf input) throws Exception {
    ByteBuf buffer = getBufferIfSufficient(input, 5, context);
    if (buffer != null) {
      decode(context, buffer);
    }
  }

  protected void decode(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf buffer) throws Exception {
    ChannelPipeline pipeline = context.pipeline();
    if (detectSsl && SslHandler.isEncrypted(buffer)) {
      SSLEngine engine = SSL_SERVER_CONTEXT.getValue().createSSLEngine();
      engine.setUseClientMode(false);
      pipeline.addLast(new SslHandler(engine), new ChunkedWriteHandler(),
                       new PortUnificationServerHandler(delegatingHttpRequestHandler, false, detectGzip));
    }
    else {
      int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
      int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
      if (detectGzip && magic1 == 31 && magic2 == 139) {
        pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP), ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP),
                         new PortUnificationServerHandler(delegatingHttpRequestHandler, detectSsl, false));
      }
      else if (isHttp(magic1, magic2)) {
        NettyUtil.addHttpServerCodec(pipeline);
        pipeline.addLast("delegatingHttpHandler", delegatingHttpRequestHandler);
        final Logger logger = Logger.getInstance(BuiltInServer.class);
        if (logger.isDebugEnabled()) {
          pipeline.addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
              if (message instanceof HttpResponse) {
                HttpResponse response = (HttpResponse)message;
                logger.debug("OUT HTTP: " + response.toString());
              }
              super.write(context, message, promise);
            }
          });
        }
      }
      else if (magic1 == 'C' && magic2 == 'H') {
        buffer.skipBytes(2);
        pipeline.addLast(new CustomHandlerDelegator());
      }
      else {
        Logger.getInstance(BuiltInServer.class).warn("unknown request, first two bytes " + magic1 + " " + magic2);
        context.close();
      }
    }

    // must be after new channels handlers addition (netty bug?)
    pipeline.remove(this);
    // Buffer will be automatically released after messageReceived, but we pass it to next handler, and next handler will also release, so, we must retain.
    // We can introduce Decoder.isAutoRelease, but in this case, if error will be thrown while we are executing, buffer will not be released.
    // So, it is robust solution just always release (Decoder does) and just retain (we - client) if autorelease behavior is not suitable.
    buffer.retain();
    // we must fire channel read - new added handler must read buffer
    context.fireChannelRead(buffer);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    NettyUtil.logAndClose(cause, Logger.getInstance(BuiltInServer.class), context.channel());
  }

  private static boolean isHttp(int magic1, int magic2) {
    return
            magic1 == 'G' && magic2 == 'E' || // GET
            magic1 == 'P' && magic2 == 'O' || // POST
            magic1 == 'P' && magic2 == 'U' || // PUT
            magic1 == 'H' && magic2 == 'E' || // HEAD
            magic1 == 'O' && magic2 == 'P' || // OPTIONS
            magic1 == 'P' && magic2 == 'A' || // PATCH
            magic1 == 'D' && magic2 == 'E' || // DELETE
            magic1 == 'T' && magic2 == 'R' || // TRACE
            magic1 == 'C' && magic2 == 'O';   // CONNECT
  }

  private static class CustomHandlerDelegator extends Decoder {
    private static final int UUID_LENGTH = 16;

    @Override
    protected void messageReceived(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf input) throws Exception {
      ByteBuf buffer = getBufferIfSufficient(input, UUID_LENGTH, context);
      if (buffer == null) {
        return;
      }

      UUID uuid = new UUID(buffer.readLong(), buffer.readLong());
      for (BinaryRequestHandler customHandler : Application.get().getExtensionList(BinaryRequestHandler.class)) {
        if (uuid.equals(customHandler.getId())) {
          ChannelPipeline pipeline = context.pipeline();
          pipeline.addLast(customHandler.getInboundHandler(context));
          pipeline.addLast(ChannelExceptionHandler.getInstance());
          pipeline.remove(this);

          context.fireChannelRead(buffer);
          break;
        }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
      NettyUtil.logAndClose(cause, Logger.getInstance(BuiltInServer.class), context.channel());
    }
  }
}