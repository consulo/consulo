/**
 * @author VISTALL
 * @since 20/01/2022
 */
module consulo.configurable.api {
  // TODO [VISTALL] obsolete dep
  requires java.desktop;

  requires transitive consulo.project.api;
  requires transitive consulo.ui.api;
  requires transitive consulo.base.localize.library;

  exports consulo.configurable;
  exports consulo.configurable.internal to consulo.ide.impl;
}