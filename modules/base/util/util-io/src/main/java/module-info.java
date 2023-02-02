/**
 * @author VISTALL
 * @since 2020-10-23
 */
module consulo.util.io {
  requires consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires org.slf4j;

  exports consulo.util.io;
  exports consulo.util.io.zip;
}