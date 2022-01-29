/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.application.api {
  requires transitive consulo.component.api;
  requires transitive consulo.localize.api;

  requires transitive consulo.util.concurrent;
  requires transitive consulo.util.collection;

  requires consulo.injecting.api;
  
  requires transitive jakarta.inject;

  // TODO [VISTALL] remove this dependency when we will ready. obsolete dep
  requires java.desktop;

  exports consulo.application;
  exports consulo.application.event;
  exports consulo.application.constraint;
  exports consulo.application.extension;
  exports consulo.application.progress;

  // TODO [VISTALL] impl package
  exports consulo.application.internal;
  exports consulo.application.internal.concurrency;

  exports consulo.application.util.function;
  exports consulo.application.util.registry;
}