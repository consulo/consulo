package consulo.remoteServer.agent;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.remoteServer.agent.shared.RemoteAgent;

import java.io.File;
import java.util.List;

/**
 * @author michael.golubev
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RemoteAgentManager {

  public static RemoteAgentManager getInstance() {
    return Application.get().getInstance(RemoteAgentManager.class);
  }

  public abstract <T extends RemoteAgent> T createAgent(RemoteAgentProxyFactory agentProxyFactory,
                                                        List<File> instanceLibraries,
                                                        List<Class<?>> commonJarClasses,
                                                        String specificsRuntimeModuleName,
                                                        String specificsBuildJarPath,
                                                        Class<T> agentInterface,
                                                        String agentClassName,
                                                        Class<?> pluginClass) throws Exception;

  public abstract RemoteAgentProxyFactory createReflectiveThreadProxyFactory(ClassLoader callerClassLoader);
}
