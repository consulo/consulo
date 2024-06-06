package consulo.remoteServer.impl.internal.agent;

import consulo.remoteServer.impl.internal.agent.CallerClassLoaderProvider;
import consulo.remoteServer.impl.internal.agent.ChildWrapperCreator;
import consulo.remoteServer.impl.internal.agent.SequentialTaskExecutor;

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
