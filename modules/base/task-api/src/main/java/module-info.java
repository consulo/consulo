/**
 * @author VISTALL
 * @since 01-Aug-22
 */
module consulo.task.api {
  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
  requires consulo.ui.ex.awt.api;

  requires transitive consulo.project.api;
  requires transitive consulo.version.control.system.api;
  requires transitive consulo.code.editor.api;
  requires consulo.language.editor.ui.api;
  requires consulo.http.api;

  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpmime;
  requires com.google.gson;

  exports consulo.task;
  exports consulo.task.context;
  exports consulo.task.event;
  exports consulo.task.ui;
  exports consulo.task.util;
  exports consulo.task.util.gson;
  exports consulo.task.internal to consulo.ide.impl;

  opens consulo.task to consulo.util.xml.serializer;
}