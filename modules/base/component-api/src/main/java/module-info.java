/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.component.api {
  requires transitive consulo.disposer.api;
  requires transitive consulo.logging.api;
  requires transitive consulo.util.dataholder;
  requires transitive consulo.util.lang;
  requires transitive consulo.util.collection.primitive;
  requires transitive consulo.util.io;
  requires transitive consulo.container.api;
  requires transitive consulo.annotation;
  requires transitive consulo.ui.api;
  requires transitive consulo.util.serializer;
  requires transitive consulo.util.concurrent;
  requires transitive consulo.util.collection;
  requires transitive consulo.platform.api;
  requires transitive org.jdom;
  
  requires consulo.injecting.api;

  exports consulo.component;
  exports consulo.component.extension;
  exports consulo.component.persist;
  exports consulo.component.messagebus;
  exports consulo.component.util;
  exports consulo.component.util.localize;
  exports consulo.component.util.pointer;
  exports consulo.component.util.text;
  exports consulo.component.util.graph;

  exports consulo.component.extension.internal to consulo.application.api, consulo.virtual.file.system.api;
}