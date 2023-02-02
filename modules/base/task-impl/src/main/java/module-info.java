/**
 * @author VISTALL
 * @since 02/02/2023
 */
module consulo.task.impl {
  requires consulo.task.api;
  requires consulo.project.ui.api;
  requires consulo.language.editor.ui.api;
  requires consulo.http.api;
  
  exports consulo.task.impl.internal to consulo.ide.impl;
  exports consulo.task.impl.internal.action to consulo.ide.impl;
  exports consulo.task.impl.internal.context to consulo.ide.impl;

  opens consulo.task.impl.internal to consulo.util.xml.serializer;

  requires java.desktop;
}