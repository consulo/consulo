/**
 * @author VISTALL
 * @since 18-Apr-22
 */
module consulo.language.editor.refactoring.api {
  requires transitive consulo.document.api;
  requires transitive consulo.find.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.language.editor.ui.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.usage.api;

  requires consulo.external.service.api;
  requires consulo.local.history.api;
  requires consulo.project.ui.view.api;

  // TODO remove this dependencies in future
  requires consulo.ui.ex.awt.api;
  requires forms.rt;
  requires java.desktop;

  exports consulo.language.editor.refactoring;
  exports consulo.language.editor.refactoring.action;
  exports consulo.language.editor.refactoring.changeSignature;
  exports consulo.language.editor.refactoring.classMember;
  exports consulo.language.editor.refactoring.copy;
  exports consulo.language.editor.refactoring.event;
  exports consulo.language.editor.refactoring.inline;
  exports consulo.language.editor.refactoring.introduce.inplace;
  exports consulo.language.editor.refactoring.localize;
  exports consulo.language.editor.refactoring.move;
  exports consulo.language.editor.refactoring.move.fileOrDirectory;
  exports consulo.language.editor.refactoring.postfixTemplate;
  exports consulo.language.editor.refactoring.rename;
  exports consulo.language.editor.refactoring.rename.inplace;
  exports consulo.language.editor.refactoring.safeDelete;
  exports consulo.language.editor.refactoring.safeDelete.usageInfo;
  exports consulo.language.editor.refactoring.ui;
  exports consulo.language.editor.refactoring.unwrap;
  exports consulo.language.editor.refactoring.util;

  exports consulo.language.editor.refactoring.internal to consulo.ide.impl;
  exports consulo.language.editor.refactoring.internal.unwrap to consulo.ide.impl;
}