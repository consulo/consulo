/**
 * @author VISTALL
 * @since 12-Mar-22
 */
module consulo.language.code.style.api {
  // todo drop this dependency
  requires java.desktop;

  requires transitive consulo.language.api;
  requires transitive consulo.color.scheme.api;
  requires transitive consulo.undo.redo.api;

  requires static consulo.ui.ex.api;
  // todo drop this dependency
  requires static consulo.ui.ex.awt.api;

  exports consulo.language.codeStyle;
  exports consulo.language.codeStyle.arrangement;
  exports consulo.language.codeStyle.event;
  exports consulo.language.codeStyle.arrangement.group;
  exports consulo.language.codeStyle.arrangement.std;
  exports consulo.language.codeStyle.arrangement.model;
  exports consulo.language.codeStyle.arrangement.match;
  exports consulo.language.codeStyle.fileSet;
  exports consulo.language.codeStyle.setting;
  exports consulo.language.codeStyle.template;
  exports consulo.language.codeStyle.inject;
  exports consulo.language.codeStyle.lineIndent;

  exports consulo.language.codeStyle.internal to consulo.ide.impl, consulo.language.editor.impl;

  opens consulo.language.codeStyle to consulo.util.xml.serializer;
}