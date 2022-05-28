/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.ide.api {
  // TODO obsolete dep
  requires java.desktop;

  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.configurable.api;
  requires transitive consulo.compiler.artifact.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.language.api;
  requires transitive consulo.code.editor.api;
  requires transitive consulo.injecting.api;
  requires transitive consulo.annotation;
  requires transitive consulo.proxy;
  requires transitive consulo.platform.api;
  requires transitive consulo.base.localize.library;
  requires transitive consulo.base.icon.library;
  requires transitive consulo.ui.api;
  requires transitive consulo.disposer.api;
  requires transitive consulo.logging.api;
  requires transitive consulo.localize.api;
  requires transitive consulo.component.api;
  requires transitive consulo.application.api;
  requires transitive consulo.application.content.api;
  requires transitive consulo.document.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.project.api;
  requires transitive consulo.file.chooser.api;
  requires transitive consulo.project.content.api;
  requires transitive consulo.language.impl;
  requires transitive consulo.index.io;
  requires transitive consulo.datacontext.api;
  requires transitive consulo.ui.ex.api;
  requires transitive consulo.project.ui.api;
  requires transitive consulo.navigation.api;
  requires transitive consulo.process.api;
  requires transitive consulo.execution.api;
  requires transitive consulo.execution.debug.api;
  requires transitive consulo.execution.coverage.api;
  requires transitive consulo.compiler.api;
  requires transitive consulo.language.editor.api;
  requires transitive consulo.undo.redo.api;
  requires transitive consulo.file.editor.api;
  requires transitive consulo.file.template.api;
  requires transitive consulo.find.api;
  requires transitive consulo.project.ui.view.api;
  requires transitive consulo.local.history.api;
  requires transitive consulo.diff.api;
  requires transitive consulo.execution.test.api;
  requires transitive consulo.path.macro.api;
  requires transitive consulo.module.ui.api;
  requires transitive consulo.language.editor.refactoring.api;
  requires transitive consulo.web.browser.api;

  exports consulo.ide;
  exports consulo.ide.navigation;
  exports consulo.ide.setting;
  exports consulo.ide.setting.module;
  exports consulo.ide.setting.bundle;
  exports consulo.ide.setting.ui;
  exports consulo.ide.setting.module.event;
  exports consulo.ide.ui;
  exports consulo.ide.ui.popup;
}