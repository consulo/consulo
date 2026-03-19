// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.util;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class NativeFileLoader {
  // TODO [VISTALL] make loading by classloader, override consulo.container.impl.classloader.PluginClassLoaderImpl.findLibrary() method

  /**
   * @param systemLoad must be referenced to System::load, this need for fixing caller from stacktrace
   *                   (from target classloader, not NativeLibraryLoader classloader)
   */
  public static void loadLibrary(String libName, Consumer<String> systemLoad) {
    Class<?> callerClass = Objects.requireNonNull(ReflectionUtil.getGrandCallerClass());

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    String libFileName = Platform.current().mapLibraryName(libName);

    String libPath;
    File nativePluginDirectory = new File(plugin.getPath(), "native");
    File libFile = new File(nativePluginDirectory, libFileName);

    if (libFile.exists()) {
      libPath = libFile.getAbsolutePath();
    }
    else {
      throw new UnsatisfiedLinkError("'" + libFileName + "' not found in '" + nativePluginDirectory + "' among " + Arrays.toString(nativePluginDirectory.list()));
    }

    systemLoad.accept(libPath);
  }

  public static File findExecutable(String fileName) {
    Class<?> callerClass = Objects.requireNonNull(ReflectionUtil.getGrandCallerClass());

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    File nativePluginDirectory = new File(plugin.getPath(), "native");

    return new File(nativePluginDirectory, fileName);
  }

  public static Path findExecutablePath(String fileName) {
    Class<?> callerClass = Objects.requireNonNull(ReflectionUtil.getGrandCallerClass());

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    @Nullable Path nioPath = plugin.getNioPath();
    if (nioPath == null) {
      throw new IllegalArgumentException("Plugin for class " + callerClass + " has null nioPath");
    }

    return nioPath.resolve("native").resolve(fileName);
  }
}
