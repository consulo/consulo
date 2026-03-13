import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-07-17
 */
@NullMarked
module consulo.util.socket.connection {
  requires org.slf4j;
  requires consulo.util.collection;
  requires consulo.util.collection.primitive;

  requires static consulo.annotation;

  exports consulo.util.socketConnection;
  exports consulo.util.socketConnection.impl;
}