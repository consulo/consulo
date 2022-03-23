/**
 * @author VISTALL
 * @since 23-Mar-22
 */
module consulo.language.editor.impl {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.language.editor.api;
  requires consulo.language.inject.impl;

  exports consulo.language.editor.impl.intention;

  exports consulo.language.editor.impl.internal.intention to consulo.ide.impl;
}