package consulo.builtInServer.impl.net.http;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.ThrowableNotNullFunction;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.builtInServer.http.HttpRequestHandler;
import consulo.builtInServer.http.Responses;
import consulo.builtInServer.http.util.HttpRequestUtil;
import consulo.builtInServer.impl.net.websocket.WebSocketHandler;
import consulo.logging.Logger;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.Imaging;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@ChannelHandler.Sharable
final class DelegatingHttpRequestHandler extends DelegatingHttpRequestHandlerBase {
  private static final AttributeKey<HttpRequestHandler> PREV_HANDLER = AttributeKey.valueOf("DelegatingHttpRequestHandler.handler");

  @Override
  protected boolean process(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder urlDecoder) throws Exception {
    ThrowableNotNullFunction<HttpRequestHandler, Boolean, IOException> checkAndProcess = httpRequestHandler -> {
      return httpRequestHandler.isSupported(request) &&
             !HttpRequestUtil.isWriteFromBrowserWithoutOrigin(request) &&
             httpRequestHandler.isAccessible(request) &&
             httpRequestHandler.process(urlDecoder, request, context);
    };


    Attribute<HttpRequestHandler> prevHandlerAttribute = context.channel().attr(PREV_HANDLER);
    HttpRequestHandler connectedHandler = prevHandlerAttribute.get();
    if (connectedHandler != null) {
      if (checkAndProcess.fun(connectedHandler)) {
        return true;
      }
      // prev cached connectedHandler is not suitable for this request, so, let's find it again
      prevHandlerAttribute.set(null);
    }

    HttpHeaders headers = request.headers();
    if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

      // adding new handler to the existing pipeline to handle WebSocket Messages
      context.pipeline().replace(this, "websocketHandler", new WebSocketHandler());
      // do the Handshake to upgrade connection from HTTP to WebSocket protocol
      handleHandshake(context, request);
      return true;
    }

    for (HttpRequestHandler handler : HttpRequestHandler.EP_NAME.getExtensionList()) {
      try {
        if (checkAndProcess.fun(handler)) {
          prevHandlerAttribute.set(handler);
          return true;
        }
      }
      catch (Throwable e) {
        Logger.getInstance(BuiltInServer.class).error(e);
      }
    }

    if (urlDecoder.path().equals("/favicon.ico")) {
      Icon icon = TargetAWT.to(Application.get().getIcon());
      BufferedImage image = UIUtil.createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      icon.paintIcon(null, image.getGraphics(), 0, 0);
      byte[] icoBytes = Imaging.writeImageToBytes(image, ImageFormats.ICO, null);

      HttpResponse response = Responses.response("image/vnd.microsoft.icon", Unpooled.wrappedBuffer(icoBytes));
      response = Responses.addNoCache(response);
      Responses.send(response, context.channel(), request);
      return true;
    }

    return false;
  }

  private void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
    WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
    WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
    }
    else {
      handshaker.handshake(ctx.channel(), req);
    }
  }

  private String getWebSocketURL(HttpRequest req) {
    return "ws://" + req.headers().get("Host") + req.uri();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    try {
      ctx.channel().attr(PREV_HANDLER).set(null);
    }
    finally {
      super.exceptionCaught(ctx, cause);
    }
  }
}
