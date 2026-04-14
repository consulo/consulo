/**
 * @author VISTALL
 * @since 19-Feb-22
 */
module consulo.ide.api {
  // TODO obsolete dep
  requires java.desktop;

  requires consulo.compiler.artifact.api;
  requires consulo.virtual.file.system.http.api;
  requires consulo.code.editor.api;
  requires consulo.language.api;
  requires consulo.file.template.api;
  requires consulo.local.history.api;
  requires consulo.ui.ex.api;
  requires consulo.ui.ex.awt.api;
  requires consulo.external.service.api;
  requires consulo.external.system.api;
  requires consulo.language.editor.api;
  requires consulo.language.editor.refactoring.api;
  requires consulo.http.api;

  exports consulo.ide;
  exports consulo.ide.action;
  exports consulo.ide.action.ui;
  exports consulo.ide.externalSystem.importing;
  exports consulo.ide.navigation;
  exports consulo.ide.tipOfDay;
  exports consulo.ide.navigationToolbar;
  exports consulo.ide.setting;
  exports consulo.ide.setting.module;
  exports consulo.ide.setting.bundle;
  exports consulo.ide.setting.module.event;
  exports consulo.ide.moduleImport;
  exports consulo.ide.newModule;
  exports consulo.ide.runAnything;
  exports consulo.ide.newModule.ui;
  exports consulo.ide.ui;
  exports consulo.ide.ui.popup;
  exports consulo.ide.localize;
  exports consulo.ide.util;

  exports consulo.ide.internal to consulo.ide.impl, consulo.sand.language.plugin;
}