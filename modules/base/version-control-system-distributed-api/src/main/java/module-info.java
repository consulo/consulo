/**
 * @author VISTALL
 * @since 31-Jul-22
 */
module consulo.version.control.system.distributed.api {
  requires transitive consulo.version.control.system.api;

  // TODO remove in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
  
  exports consulo.versionControlSystem.distributed.push;
  exports consulo.versionControlSystem.distributed.repository;
}