/**
 * @author VISTALL
 * @since 05/02/2022
 */
module consulo.code.editor.api {
  // todo not required dependency
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.document.api;
  requires transitive consulo.ui.ex.api;

  exports consulo.codeEditor;
  exports consulo.codeEditor.colorScheme;
  exports consulo.codeEditor.colorScheme.event;
  exports consulo.codeEditor.event;
  exports consulo.codeEditor.markup;
  exports consulo.codeEditor.util;
}