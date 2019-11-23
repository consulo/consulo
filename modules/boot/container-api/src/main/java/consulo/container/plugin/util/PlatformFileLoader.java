/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.plugin.util;

import consulo.util.nodep.SystemInfoRt;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2019-07-27
 */
public class PlatformFileLoader {
  @Nonnull
  public static File findFile(@Nonnull String fileName, @Nonnull Class<?> callerClass) {
    File pluginPath = PluginManager.getPluginPath(callerClass);

    File nativeDirectory = new File(pluginPath, "native");

    return new File(nativeDirectory, fileName);
  }

  public static void loadLibrary(@Nonnull String libName, @Nonnull Class<?> callerClass) {
    String libFileName = mapLibraryName(libName);

    File pluginPath = PluginManager.getPluginPath(callerClass);

    File nativeDirectory = new File(pluginPath, "native");

    File libraryInNative = new File(nativeDirectory, libFileName);

    String libPath;
    if (libraryInNative.exists()) {
      libPath = libraryInNative.getAbsolutePath();
    }
    else {
      throw new UnsatisfiedLinkError("'" + libFileName + "' not found in '" + libraryInNative + "' among " + Arrays.toString(nativeDirectory.list()));
    }

    System.load(libPath);
  }

  @Nonnull
  private static String mapLibraryName(@Nonnull String libName) {
    String baseName = libName;
    if (SystemInfoRt.is64Bit) {
      baseName = baseName.replace("32", "") + "64";
    }
    String fileName = System.mapLibraryName(baseName);
    if (SystemInfoRt.isMac) {
      fileName = fileName.replace(".jnilib", ".dylib");
    }
    return fileName;
  }
}
