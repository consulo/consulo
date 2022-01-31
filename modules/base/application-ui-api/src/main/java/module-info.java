/**
 * @author VISTALL
 * @since 31/01/2022
 */
module consulo.application.ui.api {
  // TODO obsolete dependency
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.proxy;
  requires consulo.desktop.awt.hacking;
  requires transitive kava.beans;

  requires com.sun.jna;
  requires imgscalr.lib;

  exports consulo.application.ui;
  exports consulo.application.ui.awt;
  exports consulo.application.ui.awt.internal;
  exports consulo.application.ui.awt.paint;
  exports consulo.application.ui.event;
  exports consulo.application.ui.wm;
}