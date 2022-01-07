// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.loader;

import com.intellij.openapi.util.SystemInfo;
import consulo.container.boot.ContainerPathManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

public class NativeLibraryLoader {
  public static void loadPlatformLibrary(@Nonnull String libName) {
    String libFileName = mapLibraryName(libName);

    final String libPath;
    final File libFile = new File(ContainerPathManager.get().getBinPath(), libFileName);

    if (libFile.exists()) {
      libPath = libFile.getAbsolutePath();
    }
    else {
      File libDir = new File(ContainerPathManager.get().getBinPath());
      throw new UnsatisfiedLinkError("'" + libFileName + "' not found in '" + libDir + "' among " + Arrays.toString(libDir.list()));
    }

    System.load(libPath);
  }

  @Nonnull
  public static String mapLibraryName(@Nonnull String libName) {
    String baseName = libName;
    if (SystemInfo.is64Bit) {
      baseName = baseName.replace("32", "") + "64";
    }
    String fileName = System.mapLibraryName(baseName);
    if (SystemInfo.isMac) {
      fileName = fileName.replace(".jnilib", ".dylib");
    }
    return fileName;
  }
}
