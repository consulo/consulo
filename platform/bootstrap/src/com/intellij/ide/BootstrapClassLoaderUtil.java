/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.ide;

import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.idea.Main;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class BootstrapClassLoaderUtil extends ClassUtilCore {
  private BootstrapClassLoaderUtil() {
  }

  private static Logger getLogger() {
    return Logger.getInstance(BootstrapClassLoaderUtil.class);
  }

  @NotNull
  public static UrlClassLoader initClassLoader(boolean updatePlugins) throws Exception {
    PathManager.loadProperties();

    List<URL> classpath = new ArrayList<URL>();
    addParentClasspath(classpath);
    addLibrariesFromHome(classpath);

    UrlClassLoader newClassLoader = UrlClassLoader.build().urls(classpath).allowLock().useCache().get();

    // prepare plugins
    if (updatePlugins) {
      try {
        StartupActionScriptManager.executeActionScript();
      }
      catch (IOException e) {
        Main.showMessage("Plugin Installation Error", e);
      }
    }

    Thread.currentThread().setContextClassLoader(newClassLoader);
    return newClassLoader;
  }

  private static void addParentClasspath(List<URL> aClasspathElements) throws MalformedURLException {
    ClassLoader loader = BootstrapClassLoaderUtil.class.getClassLoader();
    if (loader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader)loader;
      ContainerUtil.addAll(aClasspathElements, urlClassLoader.getURLs());
    }
  }

  private static void addLibrariesFromHome(List<URL> classpathElements) {
    final String ideaHomePath = PathManager.getHomePath();

    addAllFromLibFolder(ideaHomePath, classpathElements);
  }

  private static void addAllFromLibFolder(String folderPath, List<URL> classPath) {
    try {
      Class<BootstrapClassLoaderUtil> aClass = BootstrapClassLoaderUtil.class;
      String selfRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
      assert selfRoot != null;
      URL selfRootUrl = new File(selfRoot).getAbsoluteFile().toURI().toURL();
      classPath.add(selfRootUrl);

      File libFolder = new File(folderPath + File.separator + "lib");
      addLibraries(classPath, libFolder, selfRootUrl);
    }
    catch (MalformedURLException e) {
      getLogger().error(e);
    }
  }

  private static void addLibraries(List<URL> classPath, File fromDir, URL selfRootUrl) throws MalformedURLException {
    File[] files = fromDir.listFiles();
    if (files == null) return;

    for (File file : files) {
      if (FileUtil.isJarOrZip(file)) {
        URL url = file.toURI().toURL();
        if (!selfRootUrl.equals(url)) {
          classPath.add(url);
        }
      }
    }
  }
}
