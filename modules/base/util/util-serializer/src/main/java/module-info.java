/**
 * @author VISTALL
 * @since 2020-10-23
 */
module consulo.util.serializer {
  requires consulo.annotation;
  requires consulo.util.lang;
  requires consulo.util.collection;
  requires consulo.util.jdom;

  requires org.slf4j;

  exports com.intellij.util.xmlb;
  exports com.intellij.util.xmlb.annotations;
}