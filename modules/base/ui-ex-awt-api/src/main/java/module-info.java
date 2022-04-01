/**
 * @author VISTALL
 * @since 21-Feb-22
 */
module consulo.ui.ex.awt.api {
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.application.ui.api;
  requires transitive consulo.application.content.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.color.scheme.api;
  requires transitive consulo.base.localize.library;
  requires transitive consulo.file.chooser.api;

  requires consulo.desktop.awt.hacking;

  requires imgscalr.lib;
  requires com.sun.jna;

  exports consulo.ui.ex.awt;
  exports consulo.ui.ex.awt.accessibility;
  exports consulo.ui.ex.awt.event;
  exports consulo.ui.ex.awt.html;
  exports consulo.ui.ex.awt.paint;
  exports consulo.ui.ex.awt.dnd;
  exports consulo.ui.ex.awt.scroll;
  exports consulo.ui.ex.awt.table;
  exports consulo.ui.ex.awt.tree;
  exports consulo.ui.ex.awt.tree.table;
  exports consulo.ui.ex.awt.update;
  exports consulo.ui.ex.awt.util;
  exports consulo.ui.ex.awt.scopeChooser;
  exports consulo.ui.ex.awt.speedSearch;
  exports consulo.ui.ex.awt.valueEditor;

  exports consulo.ui.ex.awt.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.ui.ex.awt.internal.laf;

  opens consulo.ui.ex.awt.tree to consulo.util.xml.serializer;
}