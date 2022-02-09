/**
 * @author VISTALL
 * @since 05/02/2022
 */
module consulo.editor.api {
  // todo not required dependency
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.document.api;
  requires transitive consulo.ui.ex.api;

  exports consulo.editor;
  exports consulo.editor.colorScheme;
  exports consulo.editor.colorScheme.event;
  exports consulo.editor.event;
  exports consulo.editor.markup;
  exports consulo.editor.internal to consulo.ide.impl;
}