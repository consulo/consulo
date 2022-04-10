/**
 * @author VISTALL
 * @since 10-Apr-22
 */
module consulo.path.macro.impl {
  requires transitive consulo.path.macro.api;
  requires transitive consulo.file.editor.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.language.api;
  requires transitive consulo.compiler.api;

  exports consulo.pathMacro.impl.internal to consulo.injecting.pico.impl;
}