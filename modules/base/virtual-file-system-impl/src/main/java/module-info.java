/**
 * @author VISTALL
 * @since 27-Feb-22
 */
module consulo.virtual.file.system.impl {
  requires transitive consulo.virtual.file.system.api;

  requires static com.sun.jna;

  exports consulo.virtualFileSystem.impl.internal.mediator to consulo.ide.impl;
  exports consulo.virtualFileSystem.impl.internal.windows to consulo.ide.impl;
  exports consulo.virtualFileSystem.impl.internal to consulo.ide.impl;

  opens consulo.virtualFileSystem.impl.internal to consulo.injecting.pico.impl;
}