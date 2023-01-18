/**
 * @author VISTALL
 * @since 15-Mar-22
 */
module consulo.language.editor.ui.api {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.code.editor.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.usage.api;
  requires transitive consulo.ui.ex.awt.api;

  exports consulo.language.editor.ui;
  exports consulo.language.editor.ui.navigation;
  exports consulo.language.editor.ui.awt;

  exports consulo.language.editor.ui.internal to consulo.ide.impl;
}