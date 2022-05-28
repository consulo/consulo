/**
 * @author VISTALL
 * @since 27-Mar-22
 */
module consulo.file.template.impl {
  requires transitive consulo.file.template.api;
  requires transitive consulo.language.code.style.api;

  requires velocity.engine.core;

  exports consulo.fileTemplate.impl.internal to consulo.ide.impl;
  opens consulo.fileTemplate.impl.internal to consulo.injecting.pico.impl, consulo.util.xml.serializer;
}