/**
 * @author VISTALL
 * @since 21-Jul-22
 */
module consulo.execution.test.sm.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.execution.test.api;
  requires build.serviceMessages;

  exports consulo.execution.test.sm;
  exports consulo.execution.test.sm.action;
  exports consulo.execution.test.sm.runner;
  exports consulo.execution.test.sm.runner.event;
  exports consulo.execution.test.sm.runner.history;
  exports consulo.execution.test.sm.runner.state;
  exports consulo.execution.test.sm.ui;
  exports consulo.execution.test.sm.ui.statistic;

  exports consulo.execution.test.sm.internal to consulo.ide.impl;
}