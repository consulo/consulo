import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-04-23
 */
@NullMarked
module consulo.util.xml.fast.reader {
  requires static consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.io;
  requires org.slf4j;

  exports net.n3.nanoxml;
  exports consulo.util.xml.fastReader;
}