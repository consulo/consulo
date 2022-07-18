/**
 * @author VISTALL
 * @since 18-Jul-22
 */
module consulo.project.impl {
  // TODO remove in future
  requires java.desktop;

  requires consulo.component.store.impl;
  requires consulo.application.impl;
  requires consulo.module.impl;
  requires consulo.project.ui.api;

  exports consulo.project.impl.internal to consulo.ide.impl;
  exports consulo.project.impl.internal.store to consulo.ide.impl;
}