/**
 * @author VISTALL
 * @since 04-Aug-22
 */
module consulo.file.editor.impl {
  requires consulo.file.editor.api;

  exports consulo.fileEditor.impl.internal to consulo.desktop.awt.ide.impl, consulo.ide.impl;
}