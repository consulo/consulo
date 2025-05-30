/**
 * @author VISTALL
 * @since 15-Feb-22
 */
module consulo.document.impl {
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.document.api;
  requires transitive consulo.undo.redo.api;

  requires it.unimi.dsi.fastutil;

  exports consulo.document.impl to consulo.ide.impl,
          consulo.language.impl,
          consulo.test.impl,
          consulo.code.editor.impl,
          consulo.desktop.awt.ide.impl,
          consulo.language.editor.impl;
  exports consulo.document.impl.event to consulo.ide.impl,
          consulo.language.impl,
          consulo.desktop.awt.ide.impl,
          consulo.code.editor.impl;
}