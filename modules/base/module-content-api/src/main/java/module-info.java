/**
 * @author VISTALL
 * @since 21/01/2022
 */
module consulo.module.content.api {
  requires transitive consulo.application.content.api;
  requires transitive consulo.module.api;

  exports consulo.module.content;
  exports consulo.module.content.library;
  exports consulo.module.content.layer;
  exports consulo.module.content.layer.event;
  exports consulo.module.content.layer.orderEntry;
}