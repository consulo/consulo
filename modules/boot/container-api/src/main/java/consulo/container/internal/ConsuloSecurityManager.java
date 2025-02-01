/*
 * Copyright 2013-2021 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.container.internal;

import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.*;
import consulo.util.nodep.function.Getter;

import java.net.URLPermission;
import java.security.Permission;
import java.util.*;

/**
 * Left for history, due inside jdk code, SecurityManager deprecated
 * 
 * @author VISTALL
 * @since 23/10/2021
 */
public class ConsuloSecurityManager {
    public static void runPrivilegedAction(Runnable runnable) {
        runnable.run();
    }

    public static <T> T runPrivilegedAction(Getter<T> getter) {
        return getter.get();
    }

    private final ClassLoader mySystemClassLoader;
    private final ClassLoader myPlatformClassLoader;
    private final ClassLoader myPrimaryClassLoader;

    private boolean myEnabled = false;

    private URLPermission httpsUrlPermission = new URLPermission("https:*");
    private URLPermission httpUrlPermission = new URLPermission("http:*");

    private RuntimePermission getEnvPermission = new RuntimePermission("getenv.*");

    // map library->jvm class, in this case we don't check permissions
    private final Map<String, String> mySpecialNativeCalls = new HashMap<String, String>();

    private final ThreadLocal<Boolean> myPrivilegedAction = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private final Set<String> myTrustedProperties = new HashSet<String>(Arrays.asList("i18n", "org.openjdk.java.util.stream.tripwire", "net.n3.nanoxml.XMLParser"));

    public ConsuloSecurityManager() {
        mySystemClassLoader = ClassLoader.getSystemClassLoader();
        myPlatformClassLoader = ClassLoader.getPlatformClassLoader();

        // this classloader, can be created by java loader(equal to mySystemClassLoader), but can be another if running by maven
        myPrimaryClassLoader = getClass().getClassLoader();

        mySpecialNativeCalls.put("net", "java.net.AbstractPlainDatagramSocketImpl");
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    public void checkCreateClassLoader() {
        // not interest
    }

    public void checkPackageAccess(String pkg) {
        // not interest
    }

    public void checkPackageDefinition(String pkg) {
        // not interest
    }

    public void checkPermission(Permission perm) {
        checkPermission(perm, null);
    }

    public void checkPropertiesAccess() {
        checkPermission(PluginPermissionType.GET_ENV, null);
    }

    public void checkPropertyAccess(String key) {
        if (myTrustedProperties.contains(key)) {
            return;
        }

        checkPermission(PluginPermissionType.GET_ENV, "jvmenv|" + key);
    }

    public void checkPermission(Permission perm, Object context) {
        if (perm instanceof RuntimePermission) {
            if (getEnvPermission.implies(perm)) {
                checkPermission(PluginPermissionType.GET_ENV, perm.getName());
            }
            else {
                String name = perm.getName();
                if ("setSecurityManager".equals(name)) {
                    throw new SecurityException("Can't change security manager");
                }
                else if ("manageProcess".equals(name)) {
                    checkPermission(PluginPermissionType.PROCESS_MANAGE, null);
                }
            }
        }
        else if (perm instanceof URLPermission) {
            if (httpsUrlPermission.implies(perm) || httpUrlPermission.implies(perm)) {
                checkPermission(PluginPermissionType.INTERNET_URL_ACCESS, perm.getName());
            }
        }
    }

    /**
     * FIXME [VISTALL] there no sense checking {@param cmd} for correcting execute right due, at windows cmds with spaces, will return only first part before space
     */
    public void checkExec(String cmd) {
        checkPermission(PluginPermissionType.PROCESS_CREATE, cmd);
    }

    public Class<?>[] getClassContext() {
        return null;
    }

    public void checkLink(String lib) {
        Class<?>[] classContext = null;

        if (myEnabled) {
            String safeTraceCall = mySpecialNativeCalls.get(lib);
            if (safeTraceCall != null) {
                classContext = getClassContext();

                for (Class<?> aClass : classContext) {
                    if (aClass.getName().equals(safeTraceCall)) {
                        return;
                    }
                }
            }
        }

        checkPermission(PluginPermissionType.NATIVE_LIBRARY, lib, classContext);
    }

    public void checkListen(int port) {
        checkPermission(PluginPermissionType.SOCKET_BIND, "*:" + port);
    }

    public void checkConnect(String host, int port) {
        checkConnect(host, port, null);
    }

    public void checkConnect(String host, int port, Object context) {
        checkPermission(PluginPermissionType.SOCKET_CONNECT, host + ":" + port);
    }

    private void checkPermission(PluginPermissionType pluginPermissionType, String target) {
        checkPermission(pluginPermissionType, target, null);
    }

    private void checkPermission(PluginPermissionType pluginPermissionType, String target, Class<?>[] alreadyCalledContext) {
        if (!myEnabled || myPrivilegedAction.get()) {
            return;
        }

        Set<ClassLoader> classLoaders = mergeClassLoaders(alreadyCalledContext);

        for (ClassLoader classLoader : classLoaders) {
            if (classLoader instanceof PluginClassLoader) {
                // platform classloaders ignore permission checks
                if (PluginIds.isPlatformPlugin(((PluginClassLoader) classLoader).getPluginId())) {
                    continue;
                }

                PluginClassLoader pluginClassLoader = (PluginClassLoader) classLoader;

                PluginDescriptor pluginDescriptor = pluginClassLoader.getPluginDescriptor();

                PluginPermissionDescriptor permissionDescriptor = pluginDescriptor.getPermissionDescriptor(pluginPermissionType);

                if (permissionDescriptor == null) {
                    if (target != null) {
                        throw new PluginPermissionSecurityException(pluginDescriptor, pluginPermissionType, target);
                    }
                    else {
                        throw new PluginPermissionSecurityException(pluginDescriptor, pluginPermissionType);
                    }
                }
            }
            else {
                throw new SecurityException("Unknown classLoader " + classLoader);
            }
        }
    }

    private Set<ClassLoader> mergeClassLoaders(Class<?>[] alreadyCalledContext) {
        Class<?>[] classContext = alreadyCalledContext == null ? getClassContext() : alreadyCalledContext;

        Set<ClassLoader> classLoaders = new LinkedHashSet<ClassLoader>();
        for (Class<?> aClass : classContext) {
            ClassLoader classLoader = aClass.getClassLoader();
            if (classLoader == null || myPlatformClassLoader == classLoader || mySystemClassLoader == classLoader || myPrimaryClassLoader == classLoader) {
                continue;
            }

            if (classLoader.getClass().getName().equals("jdk.internal.reflect.DelegatingClassLoader")) {
                continue;
            }
            classLoaders.add(classLoader);
        }

        return classLoaders;
    }
}
