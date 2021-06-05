/**
 * @author VISTALL
 * @since 07/02/2021
 */
module consulo.util.collection.primitive {
  requires consulo.util.collection;
  requires consulo.util.lang;

  requires org.slf4j;

  exports consulo.util.collection.primitive.bytes;
  exports consulo.util.collection.primitive.ints;
  exports consulo.util.collection.primitive.longs;
  exports consulo.util.collection.primitive.doubles;
  exports consulo.util.collection.primitive.objects;

  // TODO export only to impl modules
  exports consulo.util.collection.primitive.impl;
}