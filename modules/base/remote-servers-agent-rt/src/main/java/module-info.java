/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.remote.servers.agent.rt {
  requires java.rmi;

  exports consulo.remoteServer.agent.shared;
  exports consulo.remoteServer.agent.shared.annotation;
  exports consulo.remoteServer.agent.shared.util;
  exports consulo.remoteServer.agent.shared.util.log;
}