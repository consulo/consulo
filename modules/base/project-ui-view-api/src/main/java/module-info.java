/**
 * @author VISTALL
 * @since 02-Apr-22
 */
module consulo.project.ui.view.api {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.virtual.file.status.api;
  requires transitive consulo.language.api;
  requires transitive consulo.file.editor.api;

  exports consulo.project.ui.view.tree;
  exports consulo.project.ui.view.commander;
}