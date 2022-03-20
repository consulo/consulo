/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.language.inject.impl {
  requires transitive consulo.file.editor.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.language.impl;
  requires transitive consulo.document.impl;
  requires transitive consulo.code.editor.api;

  exports consulo.language.inject.impl.internal to consulo.ide.impl;
}