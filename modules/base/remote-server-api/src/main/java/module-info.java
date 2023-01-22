/**
 * @author VISTALL
 * @since 22/01/2023
 */
module consulo.remote.server.api {
  requires transitive consulo.application.api;
  requires transitive consulo.project.api;
  requires transitive consulo.execution.api;
  requires transitive consulo.compiler.artifact.api;

  exports consulo.remoteServer;
  exports consulo.remoteServer.configuration;
  exports consulo.remoteServer.configuration.deployment;
  exports consulo.remoteServer.runtime;
  exports consulo.remoteServer.runtime.deployment;
  exports consulo.remoteServer.runtime.deployment.debug;
  exports consulo.remoteServer.runtime.local;
  exports consulo.remoteServer.runtime.log;
  exports consulo.remoteServer.runtime.ui;
}