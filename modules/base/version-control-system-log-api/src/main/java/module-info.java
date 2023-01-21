/**
 * @author VISTALL
 * @since 31-Jul-22
 */
module consulo.version.control.system.log.api {
  requires consulo.ui.ex.api;
  requires consulo.index.io;

  requires consulo.version.control.system.api;

  // TODO remove in future
  requires java.desktop;

  exports consulo.versionControlSystem.log;
  exports consulo.versionControlSystem.log.event;
  exports consulo.versionControlSystem.log.graph;
  exports consulo.versionControlSystem.log.base;
  exports consulo.versionControlSystem.log.util;
}