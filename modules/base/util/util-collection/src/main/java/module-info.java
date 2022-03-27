module consulo.util.collection {
  requires transitive consulo.annotation;
  requires transitive consulo.util.lang;

  requires org.slf4j;

  exports consulo.util.collection;
  exports consulo.util.collection.util;

  exports consulo.util.collection.impl.map to
          consulo.util.collection.via.trove,
          consulo.util.collection.primitive,
          consulo.desktop.awt.ide.impl,
          consulo.language.impl,
          consulo.ide.impl;
  exports consulo.util.collection.impl.set to consulo.util.collection.primitive;
  exports consulo.util.collection.impl to consulo.util.collection.primitive, consulo.util.collection.via.trove;

  uses consulo.util.collection.impl.CollectionFactory;
}