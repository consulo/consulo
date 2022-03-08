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

  exports consulo.ide;
  exports consulo.ide.setting;
  exports consulo.ide.setting.module;
  exports consulo.ide.setting.bundle;
  exports consulo.ide.setting.ui;
  exports consulo.ide.setting.module.event;
  exports consulo.ide.ui.popup;
}