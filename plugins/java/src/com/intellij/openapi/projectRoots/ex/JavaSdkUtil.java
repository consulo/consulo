/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.projectRoots.ex;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.rt.compiler.JavacRunner;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class JavaSdkUtil {
  @NonNls public static final String IDEA_PREPEND_RTJAR = "idea.prepend.rtjar";

  public static void addRtJar(PathsList pathsList) {
    final String ideaRtJarPath = getIdeaRtJarPath();
    if (Boolean.getBoolean(IDEA_PREPEND_RTJAR)) {
      pathsList.addFirst(ideaRtJarPath);
    }
    else {
      pathsList.addTail(ideaRtJarPath);
    }
  }

  public static List<File> getJdkClassesRoots(File home, boolean isJre) {
    FileFilter jarFileFilter = new FileFilter() {
      @Override
      @SuppressWarnings({"HardCodedStringLiteral"})
      public boolean accept(File f) {
        return !f.isDirectory() && f.getName().endsWith(".jar");
      }
    };

    File[] jarDirs;
    if (SystemInfo.isMac && !home.getName().startsWith("mockJDK")) {
      File openJdkRtJar = new File(home, "jre/lib/rt.jar");
      if (openJdkRtJar.exists() && !openJdkRtJar.isDirectory()) {
        File libDir = new File(home, "lib");
        File classesDir = openJdkRtJar.getParentFile();
        File libExtDir = new File(openJdkRtJar.getParentFile(), "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
      else {
        File libDir = new File(home, "lib");
        File classesDir = new File(home, "../Classes");
        File libExtDir = new File(libDir, "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
    }
    else {
      File libDir = isJre ? new File(home, "lib") : new File(home, "jre/lib");
      File libExtDir = new File(libDir, "ext");
      File libEndorsedDir = new File(libDir, "endorsed");
      jarDirs = new File[]{libEndorsedDir, libDir, libExtDir};
    }

    Set<String> pathFilter = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY);
    List<File> rootFiles = ContainerUtil.newArrayList();
    for (File jarDir : jarDirs) {
      if (jarDir != null && jarDir.isDirectory()) {
        File[] jarFiles = jarDir.listFiles(jarFileFilter);
        for (File jarFile : jarFiles) {
          String jarFileName = jarFile.getName();
          if (jarFileName.equals("alt-rt.jar") || jarFileName.equals("alt-string.jar")) {
            continue;  // filter out alternative implementations
          }
          String canonicalPath = getCanonicalPath(jarFile);
          if (canonicalPath == null || !pathFilter.add(canonicalPath)) {
            continue;  // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
          }
          rootFiles.add(jarFile);
        }
      }
    }

    File classesZip = new File(home, "lib/classes.zip");
    if (classesZip.isFile()) {
      rootFiles.add(classesZip);
    }

    File classesDir = new File(home, "classes");
    if (rootFiles.isEmpty() && classesDir.isDirectory()) {
      rootFiles.add(classesDir);
    }

    return rootFiles;
  }

  @Nullable
  private static String getCanonicalPath(File file) {
    try {
      return file.getCanonicalPath();
    }
    catch (IOException e) {
      return null;
    }
  }

  public static String getJunit4JarPath() {
    try {
      return PathUtil.getJarPathForClass(Class.forName("org.junit.Test"));
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getJunit3JarPath() {
    try {
      return PathUtil.getJarPathForClass(Class.forName("junit.runner.TestSuiteLoader")); //junit3 specific class
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getIdeaRtJarPath() {
    return PathUtil.getJarPathForClass(JavacRunner.class);
  }
}
