/**
 * @author VISTALL
 * @since 24-Jul-24
 */
module consulo.project.ui.impl {
  requires transitive consulo.project.ui.api;
  requires consulo.undo.redo.api;

  exports consulo.project.ui.impl.internal to
    consulo.version.control.system.impl,
    consulo.ide.impl;

  // TODO remove in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
}