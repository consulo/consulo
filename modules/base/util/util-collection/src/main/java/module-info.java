module consulo.util.collection {
  requires transitive consulo.annotation;
  requires transitive consulo.util.lang;

  exports consulo.util.collection;
  exports consulo.util.collection.util;

  // TODO exports only to impl modules
  exports consulo.util.collection.impl.map;
  exports consulo.util.collection.impl.set;
  exports consulo.util.collection.impl;

  uses consulo.util.collection.impl.CollectionFactory;
}