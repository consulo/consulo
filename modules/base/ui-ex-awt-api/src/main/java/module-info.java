/**
 * @author VISTALL
 * @since 21-Feb-22
 */
module consulo.ui.ex.awt.api {
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.application.ui.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.color.scheme.api;

  requires consulo.desktop.awt.hacking;

  requires imgscalr.lib;

  exports consulo.ui.ex.awt;
  exports consulo.ui.ex.awt.accessibility;
  exports consulo.ui.ex.awt.event;
  exports consulo.ui.ex.awt.html;
  exports consulo.ui.ex.awt.internal;
  exports consulo.ui.ex.awt.paint;
  exports consulo.ui.ex.awt.scroll;
  exports consulo.ui.ex.awt.table;
  exports consulo.ui.ex.awt.tree;
  exports consulo.ui.ex.awt.tree.table;
  exports consulo.ui.ex.awt.update;
  exports consulo.ui.ex.awt.util;
}