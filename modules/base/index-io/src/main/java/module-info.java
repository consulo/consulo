/**
 * @author VISTALL
 * @since 28/01/2022
 */
module consulo.indexing.io {
  requires transitive consulo.annotation;
  requires transitive consulo.util.collection;
  requires transitive consulo.util.collection.primitive;
  requires transitive consulo.util.io;

  requires consulo.container.api;

  exports consulo.index.io;
  exports consulo.index.io.data;
}