// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.container.impl.classloader;

import consulo.container.PluginException;
import consulo.container.impl.classloader.proxy.ProxyDescription;
import consulo.container.impl.classloader.proxy.ProxyFactory;
import consulo.container.impl.classloader.proxy.ProxyHolderClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.classloader.PluginClassLoader;
import consulo.util.nodep.classloader.UrlClassLoader;
import consulo.util.nodep.collection.ContainerUtilRt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author Eugene Zhuravlev
 */
class PluginClassLoaderImpl extends UrlClassLoader implements PluginClassLoader, ProxyHolderClassLoader {
  static {
    if (registerAsParallelCapable()) markParallelCapable(PluginClassLoaderImpl.class);
  }

  private final ClassLoader[] myParents;
  private final PluginDescriptor myPluginDescriptor;
  private final List<String> myLibDirectories;

  private ConcurrentMap<ProxyDescription, ProxyFactory> myProxyFactories = new ConcurrentHashMap<ProxyDescription, ProxyFactory>();

  /**
   * Constructor for main platform plugins, it will set parent for ClassLoader - it's need for correct parent resolving for ServiceLoader
   */
  public PluginClassLoaderImpl(@Nonnull List<URL> urls, @Nonnull ClassLoader parent, @Nonnull PluginDescriptor pluginDescriptor) {
    super(pluginDescriptor.getPluginId().getIdString(), build().urls(urls).parent(parent).urlsWithProtectionDomain(new HashSet<URL>(urls)).allowLock().useCache());
    myParents = new ClassLoader[] {parent};
    myPluginDescriptor = pluginDescriptor;
    File libDir = new File(pluginDescriptor.getPath(), "lib");
    myLibDirectories = libDir.exists() ? Collections.singletonList(libDir.getAbsolutePath()) : Collections.<String>emptyList();
  }

  public PluginClassLoaderImpl(@Nonnull List<URL> urls, @Nonnull ClassLoader[] parents, @Nonnull PluginDescriptor pluginDescriptor) {
    super(pluginDescriptor.getPluginId().getIdString(), build().urls(urls).urlsWithProtectionDomain(new HashSet<URL>(urls)).allowLock().useCache().noPreload());
    myParents = parents;
    myPluginDescriptor = pluginDescriptor;
    File libDir = new File(myPluginDescriptor.getPath(), "lib");
    myLibDirectories = libDir.exists() ? Collections.singletonList(libDir.getAbsolutePath()) : Collections.<String>emptyList();
  }

  @Override
  public Class loadClass(@Nonnull String name, boolean resolve) throws ClassNotFoundException {
    Class c = tryLoadingClass(name, resolve, null);
    if (c == null) {
      throw new ClassNotFoundException(name + " " + this);
    }
    return c;
  }

  private interface ActionWithClassloader<Result, ParameterType> {
    Result execute(String name, ClassLoader classloader, ParameterType parameter);
  }

  private abstract static class ActionWithPluginClassLoader<Result, ParameterType> {
    Result execute(String name,
                   PluginClassLoaderImpl classloader,
                   Set<ClassLoader> visited,
                   ActionWithPluginClassLoader<Result, ParameterType> actionWithPluginClassLoader,
                   ActionWithClassloader<Result, ParameterType> actionWithClassloader,
                   ParameterType parameter) {
      Result resource = doExecute(name, classloader, parameter);
      if (resource != null) return resource;
      return classloader.processResourcesInParents(name, actionWithPluginClassLoader, actionWithClassloader, visited, parameter);
    }

    protected abstract Result doExecute(String name, PluginClassLoaderImpl classloader, ParameterType parameter);
  }

  @Nullable
  private <Result, ParameterType> Result processResourcesInParents(String name,
                                                                   ActionWithPluginClassLoader<Result, ParameterType> actionWithPluginClassLoader,
                                                                   ActionWithClassloader<Result, ParameterType> actionWithClassloader,
                                                                   Set<ClassLoader> visited,
                                                                   ParameterType parameter) {
    for (ClassLoader parent : myParents) {
      if (visited == null) visited = ContainerUtilRt.<ClassLoader>newHashSet(this);
      if (!visited.add(parent)) {
        continue;
      }

      if (parent instanceof PluginClassLoaderImpl) {
        Result resource = actionWithPluginClassLoader.execute(name, (PluginClassLoaderImpl)parent, visited, actionWithPluginClassLoader, actionWithClassloader, parameter);
        if (resource != null) {
          return resource;
        }
        continue;
      }

      Result resource = actionWithClassloader.execute(name, parent, parameter);
      if (resource != null) return resource;
    }

    return null;
  }

  private static final ActionWithPluginClassLoader<Class, Void> loadClassInPluginCL = new ActionWithPluginClassLoader<Class, Void>() {
    @Override
    Class execute(String name,
                  PluginClassLoaderImpl classloader,
                  Set<ClassLoader> visited,
                  ActionWithPluginClassLoader<Class, Void> actionWithPluginClassLoader,
                  ActionWithClassloader<Class, Void> actionWithClassloader,
                  Void parameter) {
      return classloader.tryLoadingClass(name, false, visited);
    }

    @Override
    protected Class doExecute(String name, PluginClassLoaderImpl classloader, Void parameter) {
      return null;
    }
  };

  private static final ActionWithClassloader<Class, Void> loadClassInCl = new ActionWithClassloader<Class, Void>() {
    @Override
    public Class execute(String name, ClassLoader classloader, Void parameter) {
      try {
        return classloader.loadClass(name);
      }
      catch (ClassNotFoundException ignoreAndContinue) {
        // Ignore and continue
      }
      return null;
    }
  };

  // Changed sequence in which classes are searched, this is essential if plugin uses library,
  // a different version of which is used in IDEA.
  @Nullable
  private Class tryLoadingClass(@Nonnull String name, boolean resolve, @Nullable Set<ClassLoader> visited) {
    Class c = null;
    if (!mustBeLoadedByPlatform(name)) {
      c = loadClassInsideSelf(name);
    }

    if (c == null) {
      c = processResourcesInParents(name, loadClassInPluginCL, loadClassInCl, visited, null);
    }

    if (c != null) {
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }

    return null;
  }

  private static boolean mustBeLoadedByPlatform(String className) {
    if (className.startsWith("java.")) return true;
    // FIXME [VISTALL] we don't need skip kotlin runtime, we don't use it
    return false;
  }

  @Nullable
  private Class loadClassInsideSelf(@Nonnull String name) {
    synchronized (getClassLoadingLock(name)) {
      Class c = findLoadedClass(name);
      if (c != null) {
        return c;
      }

      try {
        c = _findClass(name);
      }
      catch (IncompatibleClassChangeError e) {
        throw new PluginException("While loading class " + name + ": " + e.getMessage(), e, getPluginId());
      }
      catch (UnsupportedClassVersionError e) {
        throw new PluginException("While loading class " + name + ": " + e.getMessage(), e, getPluginId());
      }
      if (c != null) {
        PluginLoadStatistics.get().addPluginClass(getPluginId());
      }

      return c;
    }
  }

  private static final ActionWithPluginClassLoader<URL, Void> findResourceInPluginCL = new ActionWithPluginClassLoader<URL, Void>() {
    @Override
    protected URL doExecute(String name, PluginClassLoaderImpl classloader, Void parameter) {
      return classloader.findOwnResource(name);
    }
  };

  private static final ActionWithClassloader<URL, Void> findResourceInCl = new ActionWithClassloader<URL, Void>() {
    @Override
    public URL execute(String name, ClassLoader classloader, Void parameter) {
      return classloader.getResource(name);
    }
  };

  @Override
  public URL findResource(String name) {
    URL resource = findOwnResource(name);
    if (resource != null) return resource;

    return processResourcesInParents(name, findResourceInPluginCL, findResourceInCl, null, null);
  }

  @Nullable
  private URL findOwnResource(String name) {
    URL resource = super.findResource(name);
    if (resource != null) return resource;
    return null;
  }

  private static final ActionWithPluginClassLoader<InputStream, Void> getResourceAsStreamInPluginCL = new ActionWithPluginClassLoader<InputStream, Void>() {
    @Override
    protected InputStream doExecute(String name, PluginClassLoaderImpl classloader, Void parameter) {
      return classloader.getOwnResourceAsStream(name);
    }
  };

  private static final ActionWithClassloader<InputStream, Void> getResourceAsStreamInCl = new ActionWithClassloader<InputStream, Void>() {
    @Override
    public InputStream execute(String name, ClassLoader classloader, Void parameter) {
      return classloader.getResourceAsStream(name);
    }
  };

  @Override
  public InputStream getResourceAsStream(String name) {
    InputStream stream = getOwnResourceAsStream(name);
    if (stream != null) return stream;

    return processResourcesInParents(name, getResourceAsStreamInPluginCL, getResourceAsStreamInCl, null, null);
  }

  @Nullable
  private InputStream getOwnResourceAsStream(String name) {
    InputStream stream = super.getResourceAsStream(name);
    if (stream != null) return stream;
    return null;
  }

  private static final ActionWithPluginClassLoader<Void, List<Enumeration<URL>>> findResourcesInPluginCL = new ActionWithPluginClassLoader<Void, List<Enumeration<URL>>>() {
    @Override
    protected Void doExecute(String name, PluginClassLoaderImpl classloader, List<Enumeration<URL>> enumerations) {
      try {
        enumerations.add(classloader.findOwnResources(name));
      }
      catch (IOException ignore) {
      }
      return null;
    }
  };

  private static final ActionWithClassloader<Void, List<Enumeration<URL>>> findResourcesInCl = new ActionWithClassloader<Void, List<Enumeration<URL>>>() {
    @Override
    public Void execute(String name, ClassLoader classloader, List<Enumeration<URL>> enumerations) {
      try {
        enumerations.add(classloader.getResources(name));
      }
      catch (IOException ignore) {
      }
      return null;
    }
  };

  @Nonnull
  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    @SuppressWarnings("unchecked") List<Enumeration<URL>> resources = new ArrayList<Enumeration<URL>>();
    resources.add(findOwnResources(name));
    processResourcesInParents(name, findResourcesInPluginCL, findResourcesInCl, null, resources);
    return new DeepEnumeration(resources.toArray(new Enumeration[resources.size()]));
  }

  @Override
  @Nonnull
  public Enumeration<URL> findOwnResources(String name) throws IOException {
    return super.findResources(name);
  }

  @Override
  protected String findLibrary(String libName) {
    if (!myLibDirectories.isEmpty()) {
      String libFileName = System.mapLibraryName(libName);
      ListIterator<String> i = myLibDirectories.listIterator(myLibDirectories.size());
      while (i.hasPrevious()) {
        File libFile = new File(i.previous(), libFileName);
        if (libFile.exists()) {
          return libFile.getAbsolutePath();
        }
      }
    }

    return null;
  }

  @Nonnull
  @Override
  public PluginId getPluginId() {
    return myPluginDescriptor.getPluginId();
  }

  @Nonnull
  @Override
  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  @Override
  public String toString() {
    return "PluginClassLoader[" + myPluginDescriptor.getPluginId() + ", " + myPluginDescriptor.getVersion() + "] " + super.toString();
  }

  @Nonnull
  @Override
  public ProxyFactory registerOrGetProxy(@Nonnull final ProxyDescription description, @Nonnull final Function<ProxyDescription, ProxyFactory> proxyFactoryFunction) {
    return myProxyFactories.computeIfAbsent(description, proxyFactoryFunction);
  }

  private static class DeepEnumeration implements Enumeration<URL> {
    private final Enumeration<URL>[] myEnumerations;
    private int myIndex;

    DeepEnumeration(Enumeration<URL>[] enumerations) {
      myEnumerations = enumerations;
    }

    @Override
    public boolean hasMoreElements() {
      while (myIndex < myEnumerations.length) {
        Enumeration<URL> e = myEnumerations[myIndex];
        if (e != null && e.hasMoreElements()) return true;
        myIndex++;
      }
      return false;
    }

    @Override
    public URL nextElement() {
      if (!hasMoreElements()) {
        throw new NoSuchElementException();
      }
      return myEnumerations[myIndex].nextElement();
    }
  }
}