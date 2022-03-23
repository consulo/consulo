/**
 * @author VISTALL
 * @since 23-Mar-22
 */
module consulo.application.impl {
  requires transitive consulo.component.impl;
  requires transitive consulo.component.store.impl;
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.document.api;
  requires transitive consulo.language.api;
  requires transitive consulo.process.api;
  requires transitive consulo.logging.api;
  requires transitive consulo.virtual.file.system.impl;
  requires consulo.util.nodep;
  requires consulo.localize.impl;
  requires consulo.ui.impl;
  requires consulo.container.impl;

  requires consulo.util.jna;

  // TODO remove this dependency in future
  requires java.desktop;
  requires java.management;

  requires args4j;
  requires org.slf4j;

  exports consulo.application.impl.internal to consulo.ide.impl, consulo.logging.log4j2.impl, consulo.desktop.awt.ide.impl, consulo.proxy;
  exports consulo.application.impl.internal.macro to consulo.ide.impl, consulo.injecting.pico.impl;
  exports consulo.application.impl.internal.progress to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.application.impl.internal.performance to consulo.ide.impl, consulo.proxy, consulo.injecting.pico.impl;
  exports consulo.application.impl.internal.plugin to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.application.impl.internal.start to consulo.desktop.awt.ide.impl, consulo.ide.impl, consulo.logging.log4j2.impl, consulo.desktop.ide.impl, consulo.injecting.pico.impl;
  exports consulo.application.impl.internal.store to consulo.ide.impl, consulo.injecting.pico.impl;
}