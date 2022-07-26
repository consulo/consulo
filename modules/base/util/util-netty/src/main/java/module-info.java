/**
 * @author VISTALL
 * @since 26-Jul-22
 */
module consulo.util.netty {
  requires static consulo.annotation;
  requires transitive consulo.util.lang;
  requires transitive consulo.util.concurrent;

  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  exports consulo.util.netty;
}