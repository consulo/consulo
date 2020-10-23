/**
 * @author VISTALL
 * @since 2020-10-23
 */
module consulo.util.jdom {
  requires consulo.annotation;
  requires consulo.util.io;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires org.slf4j;
  requires transitive org.jdom;

  requires transitive java.xml;

  exports consulo.util.jdom;
}