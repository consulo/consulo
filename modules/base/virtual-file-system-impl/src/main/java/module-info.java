/**
 * @author VISTALL
 * @since 27-Feb-22
 */
module consulo.virtual.file.system.impl {
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.util.collection;

  requires static com.sun.jna;
  requires static consulo.util.jna;

  exports consulo.virtualFileSystem.impl.internal.mediator to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.virtualFileSystem.impl.internal to consulo.ide.impl, consulo.application.content.impl;
}