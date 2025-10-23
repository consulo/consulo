// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.util;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;
import consulo.util.lang.reflect.ReflectionUtil;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

public final class NativeFileLoader {
  // TODO [VISTALL] make loading by class-loader, override consulo.container.impl.classloader.PluginClassLoaderImpl.findLibrary() method

  /**
   * @param systemLoad must be referenced to System::load, this need for fixing caller from stacktrace (from target class-loader, not NativeLibraryLoader class-loader)
   */
  public static void loadLibrary(@Nonnull String libName, Consumer<String> systemLoad) {
    Class<?> callerClass = ReflectionUtil.getGrandCallerClass();

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

  @Nonnull
  public static File findExecutable(@Nonnull String fileName) {
    Class<?> callerClass = ReflectionUtil.getGrandCallerClass();

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    File nativePluginDirectory = new File(plugin.getPath(), "native");

    return new File(nativePluginDirectory, fileName);
  }

  @Nonnull
  public static Path findExecutablePath(@Nonnull String fileName) {
    Class<?> callerClass = ReflectionUtil.getGrandCallerClass();

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    return plugin.getNioPath().resolve("native").resolve(fileName);
  }
}
