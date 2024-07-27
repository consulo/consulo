/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.version.control.system.impl {
  requires consulo.version.control.system.api;
  requires consulo.ui.ex.api;
  requires consulo.code.editor.api;
  requires consulo.project.ui.view.api;
  requires consulo.project.ui.impl;

  // TODO we need it?
  requires consulo.language.editor.api;

  exports consulo.versionControlSystem.impl.internal to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.versionControlSystem.impl.internal.action to consulo.ide.impl;

  exports consulo.versionControlSystem.impl.internal.ui.awt to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change.ui.awt to consulo.ide.impl, consulo.desktop.awt.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change.ui to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change.ui.issueLink to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change.commited to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.update to consulo.ide.impl;

  opens consulo.versionControlSystem.impl.internal.change.commited to consulo.util.xml.serializer;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
  requires microba;
}