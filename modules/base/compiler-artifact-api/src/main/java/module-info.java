/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.compiler.artifact.api {
  requires transitive consulo.compiler.api;
  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.ui.ex.awt.api;

  exports consulo.compiler.artifact;
  exports consulo.compiler.artifact.event;
  exports consulo.compiler.artifact.element;
  exports consulo.compiler.artifact.ui;
}