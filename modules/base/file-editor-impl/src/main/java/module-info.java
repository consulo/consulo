/**
 * @author VISTALL
 * @since 04-Aug-22
 */
module consulo.file.editor.impl {
  requires consulo.file.editor.api;
  requires consulo.project.ui.view.api;

  exports consulo.fileEditor.impl.internal to consulo.desktop.awt.ide.impl, consulo.ide.impl;

  opens consulo.fileEditor.impl.internal to consulo.util.xml.serializer;

  // TODO remove in future
  requires java.desktop;
}