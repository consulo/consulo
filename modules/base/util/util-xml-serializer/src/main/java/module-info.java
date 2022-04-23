/**
 * @author VISTALL
 * @since 2020-10-23
 */
module consulo.util.xml.serializer {
  requires transitive org.jdom;

  requires consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.jdom;

  requires org.slf4j;

  // todo obsolete dependency
  requires java.desktop;

  exports consulo.util.xml.serializer;
  exports consulo.util.xml.serializer.annotation;
}