/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.version.control.system.api {
  // TODO remove this in future
  requires java.desktop;
  requires microba;
  requires forms.rt;

  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.virtual.file.status.api;
  requires transitive consulo.local.history.api;

  requires consulo.application.ui.api;
  requires consulo.code.editor.api;
  requires consulo.execution.api;
  requires transitive consulo.diff.api;

  requires consulo.ui.ex.awt.api;

  exports consulo.versionControlSystem;
  exports consulo.versionControlSystem.checkin;
  exports consulo.versionControlSystem.checkout;
  exports consulo.versionControlSystem.localize;
  exports consulo.versionControlSystem.change;
  exports consulo.versionControlSystem.change.diff;
  exports consulo.versionControlSystem.base;
  exports consulo.versionControlSystem.history;
  exports consulo.versionControlSystem.ui;
  exports consulo.versionControlSystem.action;
  exports consulo.versionControlSystem.diff;
  exports consulo.versionControlSystem.rollback;
  exports consulo.versionControlSystem.versionBrowser;
  exports consulo.versionControlSystem.annotate;
  exports consulo.versionControlSystem.change.commited;
  exports consulo.versionControlSystem.merge;
  exports consulo.versionControlSystem.update;
  exports consulo.versionControlSystem.root;
  exports consulo.versionControlSystem.util;
  exports consulo.versionControlSystem.versionBrowser.ui.awt;
  exports consulo.versionControlSystem.internal to consulo.ide.impl, consulo.version.control.system.impl;

  opens consulo.versionControlSystem to consulo.util.xml.serializer;
}