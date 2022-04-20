/**
 * @author VISTALL
 * @since 16-Feb-22
 */
module consulo.undo.redo.api {
  requires transitive consulo.project.api;
  requires transitive consulo.document.api;
  requires transitive consulo.util.lang;

  exports consulo.undoRedo;
  exports consulo.undoRedo.event;
  exports consulo.undoRedo.util;
  exports consulo.undoRedo.internal to consulo.ide.impl, consulo.language.editor.refactoring.api;
}