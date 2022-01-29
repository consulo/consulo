/**
 * @author VISTALL
 * @since 29/01/2022
 */
module consulo.ui.ex.api {
  // todo obsolete dependency
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.ui.api;
  requires transitive consulo.datacontext.api;

  exports consulo.ui.ex;
  exports consulo.ui.ex.popup;
  exports consulo.ui.ex.popup.event;
}