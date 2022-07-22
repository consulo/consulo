/**
 * @author VISTALL
 * @since 23-Mar-22
 */
module consulo.language.editor.impl {
  // TODO remove this dependency in future
  requires java.desktop;
  requires forms.rt;

  requires transitive consulo.language.editor.api;
  requires transitive consulo.file.template.api;
  requires consulo.language.inject.impl;
  requires consulo.external.service.api;

  exports consulo.language.editor.impl.intention;

  exports consulo.language.editor.impl.internal.action to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.completion to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.language.editor.impl.internal.intention to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.parser to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.psi.path to consulo.ide.impl;
  exports consulo.language.editor.impl.internal.template to consulo.ide.impl;
}