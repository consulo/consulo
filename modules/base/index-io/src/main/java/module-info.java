/**
 * @author VISTALL
 * @since 28/01/2022
 */
module consulo.index.io {
  requires transitive consulo.annotation;
  requires transitive consulo.util.collection;
  requires transitive consulo.util.collection.primitive;
  requires transitive consulo.util.io;

  requires static org.lz4.java;

  requires org.slf4j;

  requires consulo.container.api;

  exports consulo.index.io;
  exports consulo.index.io.data;

  exports consulo.index.io.internal to consulo.application.impl, consulo.ide.impl;

  uses consulo.index.io.internal.LowMemoryWatcherInternal;
}