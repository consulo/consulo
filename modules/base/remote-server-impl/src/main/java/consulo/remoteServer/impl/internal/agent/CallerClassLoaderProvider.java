package consulo.remoteServer.impl.internal.agent;

import org.jspecify.annotations.Nullable;

/**
 * @author michael.golubev
 */
public class CallerClassLoaderProvider {

  private ClassLoader myCallerClassLoader;

  public CallerClassLoaderProvider(@Nullable ClassLoader callerClassLoader) {
    myCallerClassLoader = callerClassLoader;
  }

  public ClassLoader getCallerClassLoader(Class<?> classOfDefaultLoader) {
    return myCallerClassLoader == null ? classOfDefaultLoader.getClassLoader() : myCallerClassLoader;
  }
}
