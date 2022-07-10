/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.vcs.api {
  // TODO remove this in future
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.status.api;

  requires consulo.application.ui.api;
  requires consulo.code.editor.api;
  requires consulo.execution.api;

  requires consulo.ui.ex.awt.api;

  exports consulo.vcs;
  exports consulo.vcs.checkin;
  exports consulo.vcs.localize;
  exports consulo.vcs.change;
  exports consulo.vcs.history;
  exports consulo.vcs.ui;
  exports consulo.vcs.action;
  exports consulo.vcs.diff;
  exports consulo.vcs.rollback;
  exports consulo.vcs.versionBrowser;
  exports consulo.vcs.annotate;
  exports consulo.vcs.change.commited;
  exports consulo.vcs.merge;
  exports consulo.vcs.update;
  exports consulo.vcs.root;
  exports consulo.vcs.util;
  exports consulo.vcs.internal to consulo.ide.impl;
}