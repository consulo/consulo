package com.intellij.remoteServer.agent.impl;

/**
 * @author michael.golubev
 */
public class CallerClassLoaderProvider {

  private ClassLoader myCallerClassLoader;

  public CallerClassLoaderProvider(@javax.annotation.Nullable ClassLoader callerClassLoader) {
    myCallerClassLoader = callerClassLoader;
  }

  public ClassLoader getCallerClassLoader(Class<?> classOfDefaultLoader) {
    return myCallerClassLoader == null ? classOfDefaultLoader.getClassLoader() : myCallerClassLoader;
  }
}
