/**
 * @author VISTALL
 * @since 14-Apr-22
 */
module consulo.module.ui.api {
  // TODO remove in future with awt package
  requires java.desktop;
  requires consulo.ui.ex.awt.api;

  requires transitive consulo.module.api;
  requires transitive consulo.application.content.api;

  exports consulo.module.ui;
  exports consulo.module.ui.awt;
  exports consulo.module.ui.extension;
}