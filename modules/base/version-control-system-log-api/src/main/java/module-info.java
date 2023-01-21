/**
 * @author VISTALL
 * @since 31-Jul-22
 */
module consulo.version.control.system.log.api {
  requires transitive consulo.ui.ex.api;
  requires consulo.index.io;

  requires transitive consulo.version.control.system.api;

  requires consulo.external.service.api;

  // TODO remove in future
  requires java.desktop;

  exports consulo.versionControlSystem.log;
  exports consulo.versionControlSystem.log.event;
  exports consulo.versionControlSystem.log.graph;
  exports consulo.versionControlSystem.log.graph.action;
  exports consulo.versionControlSystem.log.base;
  exports consulo.versionControlSystem.log.util;
}