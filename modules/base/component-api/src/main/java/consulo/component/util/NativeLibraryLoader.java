// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.util;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;
import consulo.util.lang.reflect.ReflectionUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

public final class NativeLibraryLoader {
  // TODO [VISTALL] make loading by classloader, override consulo.container.impl.classloader.PluginClassLoaderImpl.findLibrary() method
  public static void loadLibrary(@Nonnull String libName) {
    Class<?> callerClass = ReflectionUtil.getGrandCallerClass();

    PluginDescriptor plugin = PluginManager.getPlugin(callerClass);
    if (plugin == null) {
      throw new IllegalArgumentException("Can't find plugin for class " + callerClass);
    }

    String libFileName = Platform.current().mapLibraryName(libName);

    final String libPath;
    File nativePluginDirectory = new File(plugin.getPath(), "native");
    File libFile = new File(nativePluginDirectory, libFileName);

    if (libFile.exists()) {
      libPath = libFile.getAbsolutePath();
    }
    else {
      throw new UnsatisfiedLinkError("'" + libFileName + "' not found in '" + nativePluginDirectory + "' among " + Arrays.toString(nativePluginDirectory.list()));
    }

    System.load(libPath);
  }
}
