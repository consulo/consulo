/**
 * @author VISTALL
 * @since 24-Apr-22
 */
module consulo.web.browser.api {
  // TODO obsolete dependency
  requires java.desktop;
  requires consulo.ui.ex.awt.api;

  requires transitive consulo.project.api;
  requires consulo.code.editor.api;
  requires consulo.configurable.api;
  requires consulo.virtual.file.system.api;
  requires consulo.language.api;
  requires consulo.ui.ex.api;
  requires consulo.process.api;
  requires consulo.execution.api;

  exports consulo.webBrowser;
  exports consulo.webBrowser.action;
  exports consulo.webBrowser.chrome;
}