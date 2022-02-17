/**
 * @author VISTALL
 * @since 13-Feb-22
 */
module consulo.language.editor.api {
  // TODO obsolete dep
  requires java.desktop;

  requires transitive consulo.language.api;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.undo.redo.api;
  requires consulo.util.jdom;
  
  exports consulo.language.editor;
  exports consulo.language.editor.annotation;
  exports consulo.language.editor.gutter;
  exports consulo.language.editor.highlight;
  exports consulo.language.editor.rawHighlight;
  exports consulo.language.editor.inspection;
  exports consulo.language.editor.inspection.reference;
  exports consulo.language.editor.inspection.scheme;
  exports consulo.language.editor.inspection.scheme.event;
  exports consulo.language.editor.intention;
  exports consulo.language.editor.scope;
  exports consulo.language.editor.refactoring;
  exports consulo.language.editor.refactoring.rename;
  exports consulo.language.editor.util;
}