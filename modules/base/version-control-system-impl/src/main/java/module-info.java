/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.version.control.system.impl {
  requires consulo.version.control.system.api;
  requires consulo.module.content.api;
  requires consulo.index.io;
  requires consulo.application.impl;
  requires consulo.ui.ex.api;
  requires consulo.code.editor.api;

  exports consulo.versionControlSystem.impl.internal.change.local to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal to consulo.ide.impl;
}