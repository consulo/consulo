package consulo.remoteServer.impl.internal.agent;

import consulo.remoteServer.impl.internal.agent.CallerClassLoaderProvider;
import consulo.remoteServer.impl.internal.agent.RemoteAgentClassLoaderCache;
import consulo.remoteServer.impl.internal.agent.RemoteAgentReflectiveProxyFactory;
import jakarta.annotation.Nullable;

/**
 * @author michael.golubev
 */
public class RemoteAgentReflectiveThreadProxyFactory extends RemoteAgentThreadProxyFactory {

  public RemoteAgentReflectiveThreadProxyFactory() {
    this(null, (ClassLoader)null);
  }

  public RemoteAgentReflectiveThreadProxyFactory(RemoteAgentClassLoaderCache classLoaderCache, @Nullable ClassLoader callerClassLoader) {
    this(classLoaderCache, new CallerClassLoaderProvider(callerClassLoader));
  }

  private RemoteAgentReflectiveThreadProxyFactory(RemoteAgentClassLoaderCache classLoaderCache,
                                                  CallerClassLoaderProvider callerClassLoaderProvider) {
    super(callerClassLoaderProvider, new RemoteAgentReflectiveProxyFactory(classLoaderCache, callerClassLoaderProvider), null);
  }
}
