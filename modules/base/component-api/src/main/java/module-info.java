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
  requires transitive consulo.util.xml.serializer;
  requires transitive consulo.util.concurrent;
  requires transitive consulo.util.collection;
  requires transitive consulo.platform.api;
  requires transitive consulo.util.jdom;
  requires transitive org.jdom;

  requires static consulo.hacking.java.base;
  requires static com.ibm.icu;
  
  requires consulo.injecting.api;

  requires java.compiler;

  exports consulo.component;
  exports consulo.component.bind;
  exports consulo.component.extension;
  exports consulo.component.persist;
  exports consulo.component.macro;
  exports consulo.component.messagebus;
  exports consulo.component.util;
  exports consulo.component.util.localize;
  exports consulo.component.util.pointer;
  exports consulo.component.util.text;
  exports consulo.component.util.graph;
  exports consulo.component.util.config;

  exports consulo.component.internal to
          consulo.application.api,
          consulo.virtual.file.system.api,
          consulo.datacontext.api,
          consulo.component.impl,
          consulo.application.impl,
          // TODO [VISTALL] replace it by consulo.project.impl when ready
          consulo.ide.impl,
          consulo.module.impl,
          consulo.language.editor.api;

  opens consulo.component.internal to consulo.util.xml.serializer, consulo.injecting.pico.impl;

  uses consulo.component.bind.InjectingBinding;
}