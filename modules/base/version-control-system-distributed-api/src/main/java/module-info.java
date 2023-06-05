/**
 * @author VISTALL
 * @since 31-Jul-22
 */
module consulo.version.control.system.distributed.api {
  requires transitive consulo.version.control.system.api;
  requires transitive consulo.version.control.system.log.api;
  requires transitive consulo.file.editor.api;
  requires transitive consulo.module.content.api;

  // TODO remove in future
  requires java.desktop;
  requires consulo.ui.ex.awt.api;
  
  exports consulo.versionControlSystem.distributed;
  exports consulo.versionControlSystem.distributed.action;
  exports consulo.versionControlSystem.distributed.push;
  exports consulo.versionControlSystem.distributed.repository;
  exports consulo.versionControlSystem.distributed.branch;
  exports consulo.versionControlSystem.distributed.ui;

  opens consulo.versionControlSystem.distributed.branch to consulo.util.xml.serializer;
}