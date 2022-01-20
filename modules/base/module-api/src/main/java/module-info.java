/**
 * @author VISTALL
 * @since 20/01/2022
 */
module consulo.module.api {
  // TODO [VISTALL] temp dependency
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive org.jdom;

  exports consulo.module;
  exports consulo.module.event;
  exports consulo.module.extension;
  exports consulo.module.extension.swing;
  exports consulo.module.extension.condition;
  exports consulo.module.extension.event;
  exports consulo.module.layer;
  exports consulo.module.layer.event;
  exports consulo.module.layer.orderEntry;
}