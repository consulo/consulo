/**
 * @author VISTALL
 * @since 04-Apr-22
 */
module consulo.execution.test.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.language.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.execution.api;
  requires transitive consulo.execution.debug.api;
  requires transitive consulo.diff.api;
  requires transitive consulo.local.history.api;

  exports consulo.execution.test;
  exports consulo.execution.test.autotest;
  exports consulo.execution.test.action;
  exports consulo.execution.test.stacktrace;
  exports consulo.execution.test.export;
  exports consulo.execution.test.ui;
  exports consulo.execution.test.internal to consulo.ide.impl;

  opens consulo.execution.test.autotest to consulo.util.xml.serializer;
}