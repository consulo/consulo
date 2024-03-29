package consulo.ide.impl.idea.remoteServer.agent.impl;

import consulo.ide.impl.idea.remoteServer.agent.impl.util.SequentialTaskExecutor;

import java.lang.reflect.Proxy;

/**
 * @author michael.golubev
 */
public class RemoteAgentThreadProxyCreator {

  private final CallerClassLoaderProvider myCallerClassLoaderProvider;
  private final ChildWrapperCreator myPreWrapperCreator;

  public RemoteAgentThreadProxyCreator(CallerClassLoaderProvider callerClassLoaderProvider,
                                       @jakarta.annotation.Nullable ChildWrapperCreator preWrapperCreator) {
    myPreWrapperCreator = preWrapperCreator;
    myCallerClassLoaderProvider = callerClassLoaderProvider;
  }

  public <T> T createProxy(Class<T> agentInterface, T agentInstance) {
    final SequentialTaskExecutor taskExecutor = new SequentialTaskExecutor();

    ClassLoader callerClassLoader = myCallerClassLoaderProvider.getCallerClassLoader(agentInterface);

    return agentInterface.cast(Proxy.newProxyInstance(callerClassLoader,
                                                      new Class[]{agentInterface},
                                                      new ThreadInvocationHandler(taskExecutor, callerClassLoader, agentInstance,
                                                                                  myPreWrapperCreator)));
  }
}
