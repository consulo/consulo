/**
 * @author VISTALL
 * @since 13-Feb-22
 */
module consulo.language.editor.api {
  // TODO obsolete dep
  requires java.desktop;

  requires transitive consulo.language.api;
  requires transitive consulo.language.code.style.api;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.undo.redo.api;
  requires consulo.util.jdom;
  
  exports consulo.language.editor;
  exports consulo.language.editor.annotation;
  exports consulo.language.editor.action;
  exports consulo.language.editor.colorScheme.setting;
  exports consulo.language.editor.completion;
  exports consulo.language.editor.completion.lookup;
  exports consulo.language.editor.completion.lookup.event;
  exports consulo.language.editor.documentation;
  exports consulo.language.editor.gutter;
  exports consulo.language.editor.highlight;
  exports consulo.language.editor.inject;
  exports consulo.language.editor.completion.internal to consulo.ide.impl;
  exports consulo.language.editor.highlight.usage;
  exports consulo.language.editor.rawHighlight;
  exports consulo.language.editor.inspection;
  exports consulo.language.editor.inspection.reference;
  exports consulo.language.editor.inspection.scheme;
  exports consulo.language.editor.inspection.scheme.event;
  exports consulo.language.editor.intention;
  exports consulo.language.editor.scope;
  exports consulo.language.editor.template;
  exports consulo.language.editor.template.macro;
  exports consulo.language.editor.template.context;
  exports consulo.language.editor.template.event;
  exports consulo.language.editor.surroundWith;
  exports consulo.language.editor.refactoring;
  exports consulo.language.editor.refactoring.rename;
  exports consulo.language.editor.util;

  opens consulo.language.editor.inspection.scheme to consulo.util.xml.serializer;
}