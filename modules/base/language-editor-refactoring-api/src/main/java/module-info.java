/**
 * @author VISTALL
 * @since 18-Apr-22
 */
module consulo.language.editor.refactoring.api {
  requires transitive consulo.language.editor.api;
  requires transitive consulo.usage.api;
  requires transitive consulo.ui.ex.api;

  // TODO remove this dependencies in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;

  exports consulo.language.editor.refactoring;
  exports consulo.language.editor.refactoring.rename;
  exports consulo.language.editor.refactoring.util;
}