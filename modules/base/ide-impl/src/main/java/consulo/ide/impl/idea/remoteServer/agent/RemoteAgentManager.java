package consulo.ide.impl.idea.remoteServer.agent;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.ide.ServiceManager;
import consulo.remoteServer.agent.RemoteAgent;

import java.io.File;
import java.util.List;

/**
 * @author michael.golubev
 */
@Service(ComponentScope.APPLICATION)
public abstract class RemoteAgentManager {

  public static RemoteAgentManager getInstance() {
    return ServiceManager.getService(RemoteAgentManager.class);
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
