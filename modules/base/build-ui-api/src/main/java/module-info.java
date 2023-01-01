/**
 * @author VISTALL
 * @since 01-Aug-22
 */
module consulo.build.ui.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.execution.api;

  exports consulo.build.ui;
  exports consulo.build.ui.event;
  exports consulo.build.ui.issue;
  exports consulo.build.ui.process;
  exports consulo.build.ui.progress;
}