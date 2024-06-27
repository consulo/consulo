/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.version.control.system.impl {
  requires consulo.version.control.system.api;
  requires consulo.ui.ex.api;
  requires consulo.code.editor.api;

  exports consulo.versionControlSystem.impl.internal to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.change to consulo.ide.impl;
  exports consulo.versionControlSystem.impl.internal.action to consulo.ide.impl;
}