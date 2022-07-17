/**
 * @author VISTALL
 * @since 17-Jul-22
 */
module consulo.util.socket.connnection {
  requires org.slf4j;
  requires consulo.util.collection;
  requires consulo.util.collection.primitive;

  requires static consulo.annotation;

  exports consulo.util.socketConnection;
  exports consulo.util.socketConnection.impl;
}