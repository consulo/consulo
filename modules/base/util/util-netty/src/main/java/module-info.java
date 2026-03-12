import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-07-26
 */
@NullMarked
module consulo.util.netty {
  requires static consulo.annotation;
  requires transitive consulo.util.lang;
  requires transitive consulo.util.concurrent;

  requires transitive io.netty.buffer;
  requires transitive io.netty.codec;
  requires transitive io.netty.codec.http;
  requires transitive io.netty.common;
  requires transitive io.netty.handler;
  requires transitive io.netty.resolver;
  requires transitive io.netty.transport;

  exports consulo.util.netty;
}