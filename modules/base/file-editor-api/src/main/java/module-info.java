/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.file.editor.api {
  // TODO obsolete dep
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.application.content.api;

  exports consulo.fileEditor;
  exports consulo.fileEditor.event;
  exports consulo.fileEditor.highlight;
  exports consulo.fileEditor.structureView;
  exports consulo.fileEditor.structureView.tree;
  exports consulo.fileEditor.structureView.event;
}