package consulo.ide.impl.idea.remoteServer.agent.impl;

/**
 * @author michael.golubev
 */
public class CallerClassLoaderProvider {

  private ClassLoader myCallerClassLoader;

  public CallerClassLoaderProvider(@jakarta.annotation.Nullable ClassLoader callerClassLoader) {
    myCallerClassLoader = callerClassLoader;
  }

  public ClassLoader getCallerClassLoader(Class<?> classOfDefaultLoader) {
    return myCallerClassLoader == null ? classOfDefaultLoader.getClassLoader() : myCallerClassLoader;
  }
}
