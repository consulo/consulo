/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.execution.api {
  // TODO obsolete dependency
  requires java.desktop;

  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.process.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.code.editor.api;

  exports consulo.execution;
  exports consulo.execution.configuration;
  exports consulo.execution.configuration.ui;
  exports consulo.execution.configuration.ui.event;
  exports consulo.execution.event;
  exports consulo.execution.executor;
  exports consulo.execution.runner;
  exports consulo.execution.ui;
  exports consulo.execution.ui.console;
  exports consulo.execution.ui.event;
  exports consulo.execution.ui.layout;
  exports consulo.execution.internal to consulo.ide.impl;
}