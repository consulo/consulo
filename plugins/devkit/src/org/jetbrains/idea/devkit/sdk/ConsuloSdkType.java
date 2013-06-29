/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.sdk;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import icons.DevkitIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
public class ConsuloSdkType extends SdkType {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.idea.devkit.projectRoots.IdeaJdk");
  @NonNls private static final String LIB_DIR_NAME = "lib";
  @NonNls private static final String SRC_DIR_NAME = "src";
  @NonNls private static final String PLUGINS_DIR = "plugins";

  public ConsuloSdkType() {
    super(DevKitBundle.message("sdk.title"));
  }

  @Override
  public Icon getIcon() {
    return DevkitIcons.Add_sdk;
  }

  @Nullable
  @Override
  public Icon getGroupIcon() {
    return DevkitIcons.Sdk_closed;
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return "reference.project.structure.sdk.idea";
  }

  public String suggestHomePath() {
    return PathManager.getHomePath().replace(File.separatorChar, '/');
  }

  public boolean isValidSdkHome(String path) {
    File home = new File(path);
    if (!home.exists()) {
      return false;
    }
    if (getBuildNumber(path) == null || getMainJar(path) == null) {
      return false;
    }
    return true;
  }

  @Nullable
  private static File getMainJar(String home) {

    final File libDir = new File(home, LIB_DIR_NAME);
    File f = new File(libDir, "idea.jar");
    if (f.exists()) {
      return f;
    }

    return null;
  }

  @Nullable
  @Override
  public String getVersionString(String sdkHome) {
    return getBuildNumber(sdkHome);
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    String buildNumber = getBuildNumber(sdkHome);
    return "Consulo " + (buildNumber != null ? buildNumber : "");
  }

  @Nullable
  public static String getBuildNumber(String ideaHome) {
    try {
      @NonNls final String buildTxt = "/build.txt";
      return FileUtil.loadFile(new File(ideaHome + buildTxt)).trim();
    }
    catch (IOException e) {
      return null;
    }
  }

  private static VirtualFile[] getIdeaLibrary(String home) {
    String plugins = home + File.separator + PLUGINS_DIR + File.separator;
    ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
    appendIdeaLibrary(home, result, "junit.jar");
    appendIdeaLibrary(plugins + "core", result);
    return VfsUtilCore.toVirtualFileArray(result);
  }

  private static void appendIdeaLibrary(final String libDirPath, final ArrayList<VirtualFile> result, @NonNls final String... forbidden) {
    final String path = libDirPath + File.separator + LIB_DIR_NAME;
    final JarFileSystem jfs = JarFileSystem.getInstance();
    final File lib = new File(path);
    if (lib.isDirectory()) {
      File[] jars = lib.listFiles();
      if (jars != null) {
        for (File jar : jars) {
          @NonNls String name = jar.getName();
          if (jar.isFile() && Arrays.binarySearch(forbidden, name) < 0 && (name.endsWith(".jar") || name.endsWith(".zip"))) {
            result.add(jfs.findFileByPath(jar.getPath() + JarFileSystem.JAR_SEPARATOR));
          }
        }
      }
    }
  }

  @Override
  public void setupSdkPaths(Sdk sdk) {
    final SdkModificator sdkModificator = sdk.getSdkModificator();
    final String sdkHome = sdk.getHomePath();

    final VirtualFile[] ideaLib = getIdeaLibrary(sdkHome);
    if (ideaLib != null) {
      for (VirtualFile aIdeaLib : ideaLib) {
        sdkModificator.addRoot(aIdeaLib, OrderRootType.CLASSES);
      }
    }
    addSources(new File(sdkHome), sdkModificator);

    sdkModificator.setSdkAdditionalData(new Sandbox(getDefaultSandbox()));
    sdkModificator.commitChanges();
  }

  static String getDefaultSandbox() {
    @NonNls String defaultSandbox = "";
    try {
      defaultSandbox = new File(PathManager.getSystemPath()).getCanonicalPath() + File.separator + "plugins-sandbox";
    }
    catch (IOException e) {
      //can't be on running instance
    }
    return defaultSandbox;
  }

  private static void addSources(File file, SdkModificator sdkModificator) {
    final File src = new File(new File(file, LIB_DIR_NAME), SRC_DIR_NAME);
    if (!src.exists()) return;
    File[] srcs = src.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        @NonNls final String path = pathname.getPath();
        //noinspection SimplifiableIfStatement
        if (path.contains("generics")) return false;
        return path.endsWith(".jar") || path.endsWith(".zip");
      }
    });
    for (int i = 0; srcs != null && i < srcs.length; i++) {
      File jarFile = srcs[i];
      if (jarFile.exists()) {
        JarFileSystem jarFileSystem = JarFileSystem.getInstance();
        String path = jarFile.getAbsolutePath().replace(File.separatorChar, '/') + JarFileSystem.JAR_SEPARATOR;
        jarFileSystem.setNoCopyJarForPath(path);
        VirtualFile vFile = jarFileSystem.findFileByPath(path);
        sdkModificator.addRoot(vFile, OrderRootType.SOURCES);
      }
    }
  }


  public AdditionalDataConfigurable createAdditionalDataConfigurable(final SdkModel sdkModel, SdkModificator sdkModificator) {
    return new IdeaJdkConfigurable(sdkModel, sdkModificator);
  }

  public void saveAdditionalData(SdkAdditionalData additionalData, Element additional) {
    if (additionalData instanceof Sandbox) {
      try {
        ((Sandbox)additionalData).writeExternal(additional);
      }
      catch (WriteExternalException e) {
        LOG.error(e);
      }
    }
  }

  public SdkAdditionalData loadAdditionalData(Sdk sdk, Element additional) {
    Sandbox sandbox = new Sandbox();
    try {
      sandbox.readExternal(additional);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
    return sandbox;
  }

  @Override
  public String getPresentableName() {
    return getName();
  }

  @Nullable
  public static Sdk findIdeaJdk(@Nullable Sdk jdk) {
    if (jdk == null) return null;
    if (jdk.getSdkType() instanceof ConsuloSdkType) return jdk;
    return null;
  }

  public static SdkType getInstance() {
    return SdkType.findInstance(ConsuloSdkType.class);
  }
}
