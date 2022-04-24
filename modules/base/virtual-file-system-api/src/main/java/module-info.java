/**
 * @author VISTALL
 * @since 17/01/2022
 */
module consulo.virtual.file.system.api {
  requires transitive consulo.application.api;
  requires transitive consulo.util.io;
  requires transitive consulo.proxy;

  requires consulo.base.icon.library;
  requires consulo.base.localize.library;

  requires org.jdom;

  exports consulo.virtualFileSystem;
  exports consulo.virtualFileSystem.event;
  exports consulo.virtualFileSystem.fileType;
  exports consulo.virtualFileSystem.encoding;
  exports consulo.virtualFileSystem.extension;
  exports consulo.virtualFileSystem.archive;
  exports consulo.virtualFileSystem.pointer;
  exports consulo.virtualFileSystem.light;
  exports consulo.virtualFileSystem.http;
  exports consulo.virtualFileSystem.http.event;
  exports consulo.virtualFileSystem.util;

  exports consulo.virtualFileSystem.internal to consulo.document.api, consulo.virtual.file.system.impl, consulo.ide.impl, consulo.component.store.impl;
}