/**
 * @author VISTALL
 * @since 15-Feb-22
 */
module consulo.document.impl {
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.document.api;
  requires transitive consulo.undo.redo.api;

  exports consulo.document.impl to consulo.ide.impl;
  exports consulo.document.impl.event to consulo.ide.impl;
}