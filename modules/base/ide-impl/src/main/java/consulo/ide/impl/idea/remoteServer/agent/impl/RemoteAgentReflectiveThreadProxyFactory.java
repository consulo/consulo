package consulo.ide.impl.idea.remoteServer.agent.impl;

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
