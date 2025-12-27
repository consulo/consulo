package consulo.container.internal.plugin.classloader;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.util.nodep.collection.ContainerUtilRt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 27/12/2025
 */
public class JoinPluginClassLoader extends ClassLoader {
    public static ClassLoader createJoinedClassLoader(List<PluginDescriptor> pluginDescriptors) {
        if (pluginDescriptors.isEmpty()) {
            return ClassLoader.getPlatformClassLoader();
        }

        List<PluginClassLoaderImpl> loaders = new ArrayList<>(pluginDescriptors.size());
        for (PluginDescriptor descriptor : pluginDescriptors) {
            if (descriptor.getStatus() == PluginDescriptorStatus.OK) {
                loaders.add((PluginClassLoaderImpl) descriptor.getPluginClassLoader());
            }
        }

        if (loaders.isEmpty()) {
            return ClassLoader.getPlatformClassLoader();
        }

        return new JoinPluginClassLoader(loaders);
    }

    private final List<PluginClassLoaderImpl> myParents;

    public JoinPluginClassLoader(List<PluginClassLoaderImpl> parents) {
        myParents = parents;
    }

    @Override
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = tryLoadingClass(name, resolve, null);
        if (c == null) {
            throw new ClassNotFoundException(name + " " + this);
        }
        return c;
    }

    private <Result, ParameterType> Result processResourcesInParents(String name,
                                                                     PluginClassLoaderImpl.ActionWithPluginClassLoader<Result, ParameterType> actionWithPluginClassLoader,
                                                                     PluginClassLoaderImpl.ActionWithClassloader<Result, ParameterType> actionWithClassloader,
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

    // a different version of which is used in IDEA.
    private Class tryLoadingClass(String name, boolean resolve, Set<ClassLoader> visited) {
        Class c = null;
        if (!mustBeLoadedByPlatform(name)) {
            c = loadClassInsideSelf(name);
        }

        if (c == null) {
            c = processResourcesInParents(name, PluginClassLoaderImpl.loadClassInPluginCL, PluginClassLoaderImpl.loadClassInCl, visited, null);
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

            return null;
        }
    }

    @Override
    public URL findResource(String name) {
        URL resource = findOwnResource(name);
        if (resource != null) {
            return resource;
        }

        return processResourcesInParents(name, PluginClassLoaderImpl.findResourceInPluginCL, PluginClassLoaderImpl.findResourceInCl, null, null);
    }

    private URL findOwnResource(String name) {
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream stream = getOwnResourceAsStream(name);
        if (stream != null) {
            return stream;
        }

        return processResourcesInParents(name, PluginClassLoaderImpl.getResourceAsStreamInPluginCL, PluginClassLoaderImpl.getResourceAsStreamInCl, null, null);
    }

    private InputStream getOwnResourceAsStream(String name) {
        InputStream stream = super.getResourceAsStream(name);
        if (stream != null) {
            return stream;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<URL> findResources(String name) throws IOException {
        List<Enumeration<URL>> resources = new ArrayList<>();
        processResourcesInParents(name, PluginClassLoaderImpl.findResourcesInPluginCL, PluginClassLoaderImpl.findResourcesInCl, null, resources);
        return new PluginClassLoaderImpl.DeepEnumeration(resources.toArray(new Enumeration[resources.size()]));
    }

    @Override
    public String toString() {
        return "JoinPluginClassLoader[" + myParents.stream().map(PluginClassLoaderImpl::getPluginId).toList() + "] " + super.toString();
    }
}