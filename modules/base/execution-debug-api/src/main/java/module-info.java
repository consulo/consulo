/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.execution.debug.api {
  // todo temp dependency
  requires java.desktop;

  requires transitive consulo.execution.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.language.api;

  exports consulo.execution.debug;
  exports consulo.execution.debug.attach;
  exports consulo.execution.debug.attach.osHandler;
  exports consulo.execution.debug.event;
  exports consulo.execution.debug.breakpoint;
  exports consulo.execution.debug.breakpoint.ui;
  exports consulo.execution.debug.evaluation;
  exports consulo.execution.debug.frame;
  exports consulo.execution.debug.step;
  exports consulo.execution.debug.setting;
  exports consulo.execution.debug.ui;
  exports consulo.execution.debug.frame.presentation;
}