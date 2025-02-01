// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.container.internal.plugin.classloader;

import consulo.container.PluginException;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.util.nodep.classloader.UrlClassLoader;
import consulo.util.nodep.collection.ContainerUtilRt;

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
public class PluginClassLoaderImpl extends UrlClassLoader implements PluginClassLoader, ProxyHolderClassLoader {
    static {
        if (registerAsParallelCapable()) {
            markParallelCapable(PluginClassLoaderImpl.class);
        }
    }

    private final Map<URL, Set<String>> myUrlsIndex;
    private final ClassLoader[] myParents;
    private final PluginDescriptor myPluginDescriptor;
    private final File myLibDirectory;
    private final File myNativeDirectory;

    private ConcurrentMap<ProxyDescription, ProxyFactory> myProxyFactories = new ConcurrentHashMap<>();

    public PluginClassLoaderImpl(List<URL> urls, Map<URL, Set<String>> urlsIndex, ClassLoader[] parents, PluginDescriptor pluginDescriptor) {
        super(pluginDescriptor.getPluginId().getIdString(),
            urlsIndex,
            build()
                .urls(urls)
                .urlsWithProtectionDomain(new HashSet<>(urls))
                .allowLock()
                .noPreload());
        myUrlsIndex = urlsIndex;
        myParents = parents;
        myPluginDescriptor = pluginDescriptor;
        myLibDirectory = new File(myPluginDescriptor.getPath(), "lib");
        myNativeDirectory = new File(myPluginDescriptor.getPath(), "native");
    }

    @Override
    public Map<URL, Set<String>> getUrlsIndex() {
        return myUrlsIndex;
    }

    @Override
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
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
            if (resource != null) {
                return resource;
            }
            return classloader.processResourcesInParents(name, actionWithPluginClassLoader, actionWithClassloader, visited, parameter);
        }

        protected abstract Result doExecute(String name, PluginClassLoaderImpl classloader, ParameterType parameter);
    }

    private <Result, ParameterType> Result processResourcesInParents(String name,
                                                                     ActionWithPluginClassLoader<Result, ParameterType> actionWithPluginClassLoader,
                                                                     ActionWithClassloader<Result, ParameterType> actionWithClassloader,
                                                                     Set<ClassLoader> visited,
                                                                     ParameterType parameter) {
        for (ClassLoader parent : myParents) {
            if (visited == null) {
                visited = ContainerUtilRt.<ClassLoader>newHashSet(this);
            }
            if (!visited.add(parent)) {
                continue;
            }

            if (parent instanceof PluginClassLoaderImpl) {
                Result resource = actionWithPluginClassLoader.execute(name,
                    (PluginClassLoaderImpl) parent,
                    visited,
                    actionWithPluginClassLoader,
                    actionWithClassloader,
                    parameter);
                if (resource != null) {
                    return resource;
                }
                continue;
            }

            Result resource = actionWithClassloader.execute(name, parent, parameter);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private static final ActionWithPluginClassLoader<Class, Void> loadClassInPluginCL = new ActionWithPluginClassLoader<>() {
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

    private static final ActionWithClassloader<Class, Void> loadClassInCl = new ActionWithClassloader<>() {
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
    private Class tryLoadingClass(String name, boolean resolve, Set<ClassLoader> visited) {
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
        if (className.startsWith("java.")) {
            return true;
        }
        // FIXME [VISTALL] we don't need skip kotlin runtime, we don't use it
        return false;
    }

    private Class loadClassInsideSelf(String name) {
        synchronized (getClassLoadingLock(name)) {
            Class c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            try {
                c = _findClass(name);
            }
            catch (IncompatibleClassChangeError | UnsupportedClassVersionError e) {
                throw new PluginException("While loading class " + name + ": " + e.getMessage(), e, getPluginId());
            }
            return c;
        }
    }

    private static final ActionWithPluginClassLoader<URL, Void> findResourceInPluginCL = new ActionWithPluginClassLoader<>() {
        @Override
        protected URL doExecute(String name, PluginClassLoaderImpl classloader, Void parameter) {
            return classloader.findOwnResource(name);
        }
    };

    private static final ActionWithClassloader<URL, Void> findResourceInCl = new ActionWithClassloader<>() {
        @Override
        public URL execute(String name, ClassLoader classloader, Void parameter) {
            return classloader.getResource(name);
        }
    };

    @Override
    public URL findResource(String name) {
        URL resource = findOwnResource(name);
        if (resource != null) {
            return resource;
        }

        return processResourcesInParents(name, findResourceInPluginCL, findResourceInCl, null, null);
    }


    private URL findOwnResource(String name) {
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }
        return null;
    }

    private static final ActionWithPluginClassLoader<InputStream, Void> getResourceAsStreamInPluginCL =
        new ActionWithPluginClassLoader<>() {
            @Override
            protected InputStream doExecute(String name, PluginClassLoaderImpl classloader, Void parameter) {
                return classloader.getOwnResourceAsStream(name);
            }
        };

    private static final ActionWithClassloader<InputStream, Void> getResourceAsStreamInCl = new ActionWithClassloader<>() {
        @Override
        public InputStream execute(String name, ClassLoader classloader, Void parameter) {
            return classloader.getResourceAsStream(name);
        }
    };

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream stream = getOwnResourceAsStream(name);
        if (stream != null) {
            return stream;
        }

        return processResourcesInParents(name, getResourceAsStreamInPluginCL, getResourceAsStreamInCl, null, null);
    }

    private InputStream getOwnResourceAsStream(String name) {
        InputStream stream = super.getResourceAsStream(name);
        if (stream != null) {
            return stream;
        }
        return null;
    }

    private static final ActionWithPluginClassLoader<Void, List<Enumeration<URL>>> findResourcesInPluginCL =
        new ActionWithPluginClassLoader<>() {
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

    private static final ActionWithClassloader<Void, List<Enumeration<URL>>> findResourcesInCl =
        new ActionWithClassloader<>() {
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


    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        @SuppressWarnings("unchecked") List<Enumeration<URL>> resources = new ArrayList<>();
        resources.add(findOwnResources(name));
        processResourcesInParents(name, findResourcesInPluginCL, findResourcesInCl, null, resources);
        return new DeepEnumeration(resources.toArray(new Enumeration[resources.size()]));
    }

    @Override
    public Enumeration<URL> findOwnResources(String name) throws IOException {
        return super.findResources(name);
    }

    @Override
    protected String findLibrary(String libName) {
        if (myLibDirectory.exists()) {
            String libFileName = System.mapLibraryName(libName);

            File libFile = new File(myLibDirectory, libFileName);
            if (libFile.exists()) {
                return libFile.getAbsolutePath();
            }
        }

        if (myNativeDirectory.exists()) {
            String libFileName = System.mapLibraryName(libName);

            File libFile = new File(myNativeDirectory, libFileName);
            if (libFile.exists()) {
                return libFile.getAbsolutePath();
            }
        }

        return null;
    }

    @Override
    public PluginId getPluginId() {
        return myPluginDescriptor.getPluginId();
    }

    @Override
    public PluginDescriptor getPluginDescriptor() {
        return myPluginDescriptor;
    }

    @Override
    public String toString() {
        return "PluginClassLoader[" + myPluginDescriptor.getPluginId() + ", " + myPluginDescriptor.getVersion() + "] " + super.toString();
    }

    @Override
    public ProxyFactory registerOrGetProxy(final ProxyDescription description,
                                           final Function<ProxyDescription, ProxyFactory> proxyFactoryFunction) {
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
                if (e != null && e.hasMoreElements()) {
                    return true;
                }
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