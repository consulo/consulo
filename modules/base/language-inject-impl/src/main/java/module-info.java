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
  requires transitive consulo.code.editor.api;

  exports consulo.language.inject.impl.internal to consulo.ide.impl,
    consulo.desktop.awt.ide.impl,
    consulo.language.editor.impl,
    consulo.language.editor.refactoring.api,
    consulo.language.inject.advanced.impl;

  opens consulo.language.inject.impl.internal to consulo.language.impl, consulo.application.impl;
}