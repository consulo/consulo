/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.execution.api {
  // TODO obsolete dependency
  requires java.desktop;

  requires transitive consulo.module.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.process.api;

  exports consulo.execution;
  exports consulo.execution.configuration;
  exports consulo.execution.event;
  exports consulo.execution.runner;
  exports consulo.execution.ui;
  exports consulo.execution.ui.event;
  exports consulo.execution.ui.layout;
}