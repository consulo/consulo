/**
 * @author VISTALL
 * @since 13-Jun-22
 */
module consulo.language.copyright.api {
  // TODO remove this dependency in future
  requires java.desktop;
  requires forms.rt;
  requires consulo.ui.ex.awt.api;

  requires transitive consulo.language.api;
  requires consulo.code.editor.api;

  exports consulo.language.copyright;
  exports consulo.language.copyright.config;
  exports consulo.language.copyright.ui;
  exports consulo.language.copyright.util;
  exports consulo.language.copyright.internal to consulo.ide.impl;
}