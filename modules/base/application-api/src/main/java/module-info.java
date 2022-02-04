/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.application.api {
  // TODO [VISTALL] remove this dependency when we will ready. obsolete dep
  requires java.desktop;

  requires transitive consulo.component.api;
  requires transitive consulo.localize.api;
  requires transitive consulo.base.icon.library;

  requires transitive consulo.util.concurrent;
  requires transitive consulo.util.collection;

  requires consulo.injecting.api;
  
  requires transitive jakarta.inject;

  requires com.sun.jna;
  requires com.sun.jna.platform;

  exports consulo.application;
  exports consulo.application.dumb;
  exports consulo.application.event;
  exports consulo.application.constraint;
  exports consulo.application.extension;
  exports consulo.application.progress;

  exports consulo.application.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.application.internal.concurrency;

  exports consulo.application.util.mac.foundation;

  exports consulo.application.util;
  exports consulo.application.util.function;
  exports consulo.application.util.registry;
}