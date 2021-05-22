module consulo.util.collection {
  requires transitive consulo.annotation;
  requires transitive consulo.util.lang;

  exports consulo.util.collection;

  exports consulo.util.collection.impl.map to consulo.util.collection.primitive;
  exports consulo.util.collection.impl.set to consulo.util.collection.primitive;
  exports consulo.util.collection.impl to consulo.util.collection.primitive;
}