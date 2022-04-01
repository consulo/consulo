/**
 * @author VISTALL
 * @since 28-Mar-22
 */
module consulo.virtual.file.status.impl {
  requires transitive consulo.virtual.file.status.api;
  requires transitive consulo.code.editor.api;

  exports consulo.virtualFileSystem.status.impl.internal to consulo.injecting.pico.impl, consulo.ide.impl;
}