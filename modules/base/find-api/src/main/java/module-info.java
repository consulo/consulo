/**
 * @author VISTALL
 * @since 31-Mar-22
 */
module consulo.find.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.application.content.api;
  requires transitive consulo.language.api;
  requires transitive consulo.file.editor.api;
  requires transitive consulo.usage.api;
  requires transitive consulo.ui.ex.awt.api;

  exports consulo.find;
  exports consulo.find.ui;
}