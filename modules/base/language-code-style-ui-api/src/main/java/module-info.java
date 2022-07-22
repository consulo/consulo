/**
 * @author VISTALL
 * @since 21-Jul-22
 */
module consulo.language.code.style.ui.api {
  // TODO remove in future
  requires java.desktop;

  requires transitive consulo.language.code.style.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.ui.ex.awt.api;

  requires consulo.diff.api;

  exports consulo.language.codeStyle.ui.setting;
  exports consulo.language.codeStyle.ui.setting.arrangement;

  exports consulo.language.codeStyle.ui.internal to consulo.ide.impl;
  exports consulo.language.codeStyle.ui.internal.arrangement to consulo.ide.impl;
}