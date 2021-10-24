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
package consulo.container.impl.securityManager;

import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.*;

import java.security.Permission;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 23/10/2021
 */
public class ConsuloSecurityManager extends SecurityManager {
  private final ClassLoader mySystemClassLoader;
  private final ClassLoader myPlatformClassLoader;

  private boolean myEnabled = false;

  public ConsuloSecurityManager() {
    mySystemClassLoader = ClassLoader.getSystemClassLoader();
    myPlatformClassLoader = ClassLoader.getPlatformClassLoader();
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  @Override
  public void checkCreateClassLoader() {
    // not interest
  }

  @Override
  public void checkPackageAccess(String pkg) {
    // not interest
  }

  @Override
  public void checkPackageDefinition(String pkg) {
    // not interest
  }

  @Override
  public void checkPermission(Permission perm) {
    checkPermission(perm, null);
  }

  @Override
  public void checkPermission(Permission perm, Object context) {
    if (perm instanceof RuntimePermission) {
      String name = perm.getName();
      if ("setSecurityManager".equals(name)) {
        throw new SecurityException("Can't change security manager");
      }
    }
  }

  /**
   * FIXME [VISTALL] there no sense checking {@param cmd} for correcting execute right due, at windows cmds with spaces, will return only first part before space
   */
  @Override
  public void checkExec(String cmd) {
    checkPermission(PluginPermissionType.PROCESS);
  }

  @Override
  public void checkLink(String lib) {
    checkPermission(PluginPermissionType.NATIVE_LIBRARY);
  }

  @Override
  public void checkListen(int port) {
    checkPermission(PluginPermissionType.SOCKET);
  }

  @Override
  public void checkConnect(String host, int port) {
    checkConnect(host, port, null);
  }

  @Override
  public void checkConnect(String host, int port, Object context) {
    checkPermission(PluginPermissionType.SOCKET);
  }

  private void checkPermission(PluginPermissionType pluginPermissionType) {
    if (!myEnabled) {
      return;
    }

    Set<ClassLoader> classLoaders = mergeClassLoaders();

    for (ClassLoader classLoader : classLoaders) {
      if (classLoader instanceof PluginClassLoader) {
        // platform classloaders ignore permission checks
        if (PluginIds.isPlatformPlugin(((PluginClassLoader)classLoader).getPluginId())) {
          continue;
        }

        PluginClassLoader pluginClassLoader = (PluginClassLoader)classLoader;

        PluginDescriptor pluginDescriptor = pluginClassLoader.getPluginDescriptor();

        PluginPermissionDescriptor permissionDescriptor = pluginDescriptor.getPermissionDescriptor(pluginPermissionType);

        if (permissionDescriptor == null) {
          throw new PluginPermissionSecurityException(pluginDescriptor, pluginPermissionType);
        }
      }
      else {
        throw new SecurityException("Unknown classLoader " + classLoader);
      }
    }
  }

  private Set<ClassLoader> mergeClassLoaders() {
    Class<?>[] classContext = getClassContext();

    Set<ClassLoader> classLoaders = new LinkedHashSet<ClassLoader>();
    for (Class<?> aClass : classContext) {
      ClassLoader classLoader = aClass.getClassLoader();
      if (classLoader == null || myPlatformClassLoader == classLoader || mySystemClassLoader == classLoader) {
        continue;
      }
      classLoaders.add(classLoader);
    }

    return classLoaders;
  }
}
