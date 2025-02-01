/**
 * @author VISTALL
 * @since 22-Mar-22
 */
module consulo.component.store.impl {
  requires transitive consulo.component.impl;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.util.io;
  requires transitive consulo.component.store.api;

  requires org.lz4.java;

  exports consulo.component.store.impl.internal to consulo.ide.impl, consulo.application.impl, consulo.desktop.awt.ide.impl, consulo.proxy, consulo.component.impl, consulo.test.impl, consulo.module.impl, consulo.project.impl;
  exports consulo.component.store.impl.internal.storage to consulo.ide.impl, consulo.application.impl, consulo.component.impl, consulo.project.impl;
  exports consulo.component.store.impl.internal.scheme to consulo.application.impl;
}