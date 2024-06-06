/**
 * @author VISTALL
 * @since 22/01/2023
 */
module consulo.remote.server.impl {
  requires transitive consulo.remote.server.api;
  requires consulo.execution.debug.api;
  requires consulo.remote.servers.agent.rt;

  exports consulo.remoteServer.impl.internal.configuration to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime.deployment to consulo.ide.impl;
  exports consulo.remoteServer.impl.internal.runtime.log to consulo.ide.impl;

  opens consulo.remoteServer.impl.internal.configuration to consulo.util.xml.serializer;
  opens consulo.remoteServer.impl.internal.ui.tree.action to consulo.component.impl;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
}