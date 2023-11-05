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
  exports consulo.virtualFileSystem.fileType.localize;
  exports consulo.virtualFileSystem.fileType.matcher;
  exports consulo.virtualFileSystem.encoding;
  exports consulo.virtualFileSystem.archive;
  exports consulo.virtualFileSystem.pointer;
  exports consulo.virtualFileSystem.light;
  exports consulo.virtualFileSystem.http;
  exports consulo.virtualFileSystem.http.event;
  exports consulo.virtualFileSystem.util;
  exports consulo.virtualFileSystem.localize;

  exports consulo.virtualFileSystem.internal.core.local to consulo.test.impl;
  exports consulo.virtualFileSystem.internal.matcher to consulo.ide.impl, consulo.virtual.file.system.impl, consulo.extension.preview.recorder.impl;
  exports consulo.virtualFileSystem.internal to consulo.document.api,
              consulo.virtual.file.system.impl,
              consulo.ide.impl, consulo.component.store.impl,
              consulo.application.impl,
              consulo.test.impl,
              consulo.language.impl;
}