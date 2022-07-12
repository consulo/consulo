import consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade;

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
  requires transitive consulo.document.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.color.scheme.api;
  requires transitive consulo.base.localize.library;
  requires transitive consulo.file.chooser.api;
  requires transitive kava.beans;

  exports consulo.ui.ex;
  exports consulo.ui.ex.popup;
  exports consulo.ui.ex.popup.event;
  exports consulo.ui.ex.action;
  exports consulo.ui.ex.action.event;
  exports consulo.ui.ex.action.util;
  exports consulo.ui.ex.content;
  exports consulo.ui.ex.update;
  exports consulo.ui.ex.content.event;
  exports consulo.ui.ex.tree;
  exports consulo.ui.ex.util;
  exports consulo.ui.ex.event;
  exports consulo.ui.ex.concurrent;
  exports consulo.ui.ex.awtUnsafe;
  exports consulo.ui.ex.keymap;
  exports consulo.ui.ex.keymap.event;
  exports consulo.ui.ex.keymap.util;
  exports consulo.ui.ex.toolWindow;
  exports consulo.ui.ex.toolWindow.action;
  exports consulo.ui.ex.dialog;
  exports consulo.ui.ex.wizard;
  exports consulo.ui.ex.errorTreeView;

  exports consulo.ui.ex.internal to consulo.ide.impl;
  exports consulo.ui.ex.awtUnsafe.internal to consulo.desktop.awt.ide.impl, consulo.desktop.swt.ide.impl;

  uses TargetAWTFacade;
}