/**
 * @author VISTALL
 * @since 17/01/2022
 */
module consulo.virtual.file.system.api {
  requires transitive consulo.application.api;
  requires transitive consulo.util.io;

  exports consulo.virtualFileSystem;
  exports consulo.virtualFileSystem.event;
  exports consulo.virtualFileSystem.fileType;
  exports consulo.virtualFileSystem.encoding;
  exports consulo.virtualFileSystem.util;

  // TODO [VISTALL] impl package
  exports consulo.virtualFileSystem.internal;
}