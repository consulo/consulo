/**
 * @author VISTALL
 * @since 29/01/2022
 */
module consulo.project.ui.api {
  // todo obsolete dep
  requires java.desktop;

  requires transitive consulo.application.ui.api;
  requires transitive consulo.project.api;
  requires transitive consulo.ui.ex.api;
  requires transitive kava.beans;

  exports consulo.project.ui;
  exports consulo.project.ui.wm;
  exports consulo.project.ui.wm.event;

  exports consulo.project.ui.wm.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl;
}