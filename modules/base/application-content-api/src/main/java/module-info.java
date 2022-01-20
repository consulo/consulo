/**
 * @author VISTALL
 * @since 20/01/2022
 */
module consulo.application.content.api {
  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.base.icon.library;
  
  requires org.jdom;

  exports consulo.content;
  exports consulo.content.bundle;
  exports consulo.content.bundle.event;
}