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
  requires transitive consulo.project.ui.api;
  requires consulo.external.service.api;
  requires consulo.process.api;
  requires consulo.util.jna;

  requires static consulo.desktop.awt.eawt.wrapper;

  requires static consulo.desktop.awt.hacking;

  requires static imgscalr.lib;
  requires static com.sun.jna;
  requires static com.sun.jna.platform;

  exports consulo.ui.ex.awt;
  exports consulo.ui.ex.awt.action;
  exports consulo.ui.ex.awt.accessibility;
  exports consulo.ui.ex.awt.event;
  exports consulo.ui.ex.awt.html;
  exports consulo.ui.ex.awt.paint;
  exports consulo.ui.ex.awt.dnd;
  exports consulo.ui.ex.awt.scroll;
  exports consulo.ui.ex.awt.table;
  exports consulo.ui.ex.awt.tree;
  exports consulo.ui.ex.awt.tree.action;
  exports consulo.ui.ex.awt.tree.table;
  exports consulo.ui.ex.awt.update;
  exports consulo.ui.ex.awt.util;
  exports consulo.ui.ex.awt.scopeChooser;
  exports consulo.ui.ex.awt.speedSearch;
  exports consulo.ui.ex.awt.valueEditor;

  exports consulo.ui.ex.awt.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.language.editor.refactoring.api, consulo.language.editor.ui.api, consulo.ide.api;
  exports consulo.ui.ex.awt.internal.laf;

  opens consulo.ui.ex.awt.tree to consulo.util.xml.serializer;
}