/**
 * @author VISTALL
 * @since 02-Apr-22
 */
module consulo.coverage.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.project.ui.view.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.execution.api;
  requires transitive consulo.xcoverage.rt;

  exports consulo.coverage;
  exports consulo.coverage.view;
}