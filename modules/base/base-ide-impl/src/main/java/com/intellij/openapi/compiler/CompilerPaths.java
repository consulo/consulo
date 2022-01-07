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
package com.intellij.openapi.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Comparator;
import java.util.Locale;

/**
 * A set of utility methods for working with paths
 */
public class CompilerPaths {
  private static final Logger LOG = Logger.getInstance(CompilerPaths.class);
  private static volatile String ourSystemPath;
  public static final Comparator<String> URLS_COMPARATOR = new Comparator<String>() {
    public int compare(String o1, String o2) {
      return o1.compareTo(o2);
    }
  };
  private static final String DEFAULT_GENERATED_DIR_NAME = "generated";

  /**
   * Returns a directory
   *
   * @param project
   * @param compiler
   * @return a directory where compiler may generate files. All generated files are not deleted when the application exits
   */
  public static File getGeneratedDataDirectory(Project project, Compiler compiler) {
    //noinspection HardCodedStringLiteral
    return new File(getGeneratedDataDirectory(project), compiler.getDescription().replaceAll("\\s+", "_"));
  }

  /**
   * @param project
   * @return a root directory where generated files for various compilers are stored
   */
  public static File getGeneratedDataDirectory(Project project) {
    //noinspection HardCodedStringLiteral
    return new File(getCompilerSystemDirectory(project), ".generated");
  }

  /**
   * @param project
   * @return a root directory where compiler caches for the given project are stored
   */
  public static File getCacheStoreDirectory(final Project project) {
    //noinspection HardCodedStringLiteral
    return new File(getCompilerSystemDirectory(project), ".caches");
  }

  public static File getRebuildMarkerFile(Project project) {
    return new File(getCompilerSystemDirectory(project), "rebuild_required");
  }

  /**
   * @param project
   * @return a directory under IDEA "system" directory where all files related to compiler subsystem are stored (such as compiler caches or generated files)
   */
  public static File getCompilerSystemDirectory(Project project) {
    return getCompilerSystemDirectory(getCompilerSystemDirectoryName(project));
  }

  public static File getCompilerSystemDirectory(String compilerProjectDirName) {
    return new File(getCompilerSystemDirectory(), compilerProjectDirName);
  }

  public static String getCompilerSystemDirectoryName(Project project) {
    return getPresentableName(project) + "" + project.getLocationHash();
  }

  @Nullable
  private static String getPresentableName(final Project project) {
    if (project.isDefault()) {
      return project.getName();
    }

    String location = project.getPresentableUrl();
    if (location == null) {
      return null;
    }

    String projectName = FileUtil.toSystemIndependentName(location);
    if (projectName.endsWith("/")) {
      projectName = projectName.substring(0, projectName.length() - 1);
    }

    final int lastSlash = projectName.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash + 1 < projectName.length()) {
      projectName = projectName.substring(lastSlash + 1);
    }

    projectName = projectName.toLowerCase(Locale.US).replace(':', '_'); // replace ':' from windows drive names
    return projectName;
  }

  public static File getCompilerSystemDirectory() {
    //noinspection HardCodedStringLiteral
    final String systemPath =
      ourSystemPath != null ? ourSystemPath : (ourSystemPath = PathUtil.getCanonicalPath(ContainerPathManager.get().getSystemPath()));
    return new File(systemPath, "compiler");
  }

  @NonNls
  @Nonnull
  public static String getGenerationOutputPath(IntermediateOutputCompiler compiler, Module module, final boolean forTestSources) {
    final String generatedCompilerDirectoryPath = getGeneratedDataDirectory(module.getProject(), compiler).getPath();
    String moduleHash = null;
    String moduleDirPath = module.getModuleDirPath();
    if(moduleDirPath != null) {
      moduleHash = Integer.toHexString(moduleDirPath.hashCode());
    }
    else {
      moduleHash = module.getProject().getLocationHash();
    }
    final String moduleDir = module.getName().replaceAll("\\s+", "_") + "" + moduleHash;
    return generatedCompilerDirectoryPath.replace(File.separatorChar, '/') +
           "/" +
           moduleDir +
           "/" +
           (forTestSources ? "test" : "production");
  }
}
