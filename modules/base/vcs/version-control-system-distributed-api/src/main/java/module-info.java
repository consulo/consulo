/**
 * @author VISTALL
 * @since 31-Jul-22
 */
module consulo.vcs.distributed.api {
  requires transitive consulo.vcs.api;

  // TODO remove in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
  
  exports consulo.versionControlSystem.distributed.push;
  exports consulo.versionControlSystem.distributed.repository;
}