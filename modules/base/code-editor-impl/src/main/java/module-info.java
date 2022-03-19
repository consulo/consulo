/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.code.editor.impl {
  // TODO remove in future this dependency
  requires java.desktop;

  requires transitive consulo.code.editor.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.document.impl;
  requires transitive consulo.color.scheme.impl;

  requires consulo.injecting.api;

  exports consulo.codeEditor.impl to consulo.ide.impl, consulo.desktop.ide.impl;
}