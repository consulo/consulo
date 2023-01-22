/**
 * @author VISTALL
 * @since 22/01/2023
 */
module consulo.remote.server.impl {
  requires transitive consulo.remote.server.api;
  requires consulo.execution.debug.api;

  exports consulo.remoteServer.impl.internal.configuration to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime.deployment to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime.log to consulo.ide.impl;
}