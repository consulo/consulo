/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.language.inject.impl {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.file.editor.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.language.impl;
  requires transitive consulo.document.impl;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.code.editor.impl;
  requires consulo.application.impl;

  exports consulo.language.inject.impl.internal to consulo.ide.impl, consulo.language.editor.impl, consulo.injecting.pico.impl;

  opens consulo.language.inject.impl.internal to consulo.language.impl;
}