/**
 * @author VISTALL
 * @since 23-Mar-22
 */
module consulo.language.editor.impl {
  // TODO remove this dependency in future
  requires java.desktop;

  requires transitive consulo.language.editor.api;
  requires transitive consulo.file.template.api;
  requires consulo.language.inject.impl;

  exports consulo.language.editor.impl.intention;
  exports consulo.language.editor.impl.fileTemplate;
  exports consulo.language.editor.impl.util;

  exports consulo.language.editor.impl.internal.completion to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.language.editor.impl.internal.intention to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.parser to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.psi.path to consulo.ide.impl;

  opens consulo.language.editor.impl.internal.parser to consulo.injecting.pico.impl;
  opens consulo.language.editor.impl.internal.template to consulo.injecting.pico.impl;
}