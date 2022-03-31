/**
 * @author VISTALL
 * @since 27-Mar-22
 */
module consulo.usage.api {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.language.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.file.editor.api;
  requires transitive consulo.virtual.file.status.api;
  requires transitive consulo.project.ui.api;

  exports consulo.usage;
  exports consulo.usage.rule;
  exports consulo.usage.internal to consulo.ide.impl;
}