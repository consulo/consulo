/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.debugger.api {
  // todo temp dependency
  requires java.desktop;

  requires transitive consulo.execution.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.language.api;

  exports consulo.debugger;
  exports consulo.debugger.event;
  exports consulo.debugger.breakpoint;
  exports consulo.debugger.breakpoint.ui;
  exports consulo.debugger.evaluation;
  exports consulo.debugger.frame;
  exports consulo.debugger.step;
  exports consulo.debugger.setting;
  exports consulo.debugger.ui;
  exports consulo.debugger.frame.presentation;
}