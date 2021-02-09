/**
 * @author VISTALL
 * @since 2020-10-24
 */
module consulo.util.dataholder {
  requires consulo.annotation;
  requires consulo.util.concurrent;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.collection.primitive;

  exports consulo.util.dataholder;
  exports consulo.util.dataholder.keyFMap;
}