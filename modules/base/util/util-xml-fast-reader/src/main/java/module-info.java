/**
 * @author VISTALL
 * @since 23-Apr-22
 */
module consulo.util.xml.fast.reader {
  requires static consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.io;
  requires org.slf4j;

  exports net.n3.nanoxml;
  exports consulo.util.xml.fastReader;
}