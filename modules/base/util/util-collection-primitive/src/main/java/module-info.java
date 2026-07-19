import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2021-02-07
 */
@NullMarked
module consulo.util.collection.primitive {
  requires consulo.util.collection;
  requires consulo.util.lang;

  requires org.slf4j;
  requires transitive it.unimi.dsi.fastutil;

  exports consulo.util.collection.primitive.bytes;
  exports consulo.util.collection.primitive.ints;
  exports consulo.util.collection.primitive.longs;
  exports consulo.util.collection.primitive.doubles;
  exports consulo.util.collection.primitive.objects;
}
