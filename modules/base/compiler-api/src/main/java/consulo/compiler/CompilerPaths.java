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
package consulo.compiler;

import consulo.application.Application;
import consulo.application.util.function.Computable;
import consulo.container.boot.ContainerPathManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A set of utility methods for working with paths
 */
public class CompilerPaths {
  private static final Logger LOG = Logger.getInstance(CompilerPaths.class);

  private static volatile String ourSystemPath;
  public static final Comparator<String> URLS_COMPARATOR = (o1, o2) -> o1.compareTo(o2);
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
    final String systemPath = ourSystemPath != null ? ourSystemPath : (ourSystemPath = FileUtil.toCanonicalPath(ContainerPathManager.get().getSystemPath()));
    return new File(systemPath, "compiler");
  }

  @Nonnull
  public static String getGenerationOutputPath(IntermediateOutputCompiler compiler, Module module, final boolean forTestSources) {
    final String generatedCompilerDirectoryPath = getGeneratedDataDirectory(module.getProject(), compiler).getPath();
    String moduleHash = null;
    String moduleDirPath = module.getModuleDirPath();
    if (moduleDirPath != null) {
      moduleHash = Integer.toHexString(moduleDirPath.hashCode());
    }
    else {
      moduleHash = module.getProject().getLocationHash();
    }
    final String moduleDir = module.getName().replaceAll("\\s+", "_") + "" + moduleHash;
    return generatedCompilerDirectoryPath.replace(File.separatorChar, '/') + "/" + moduleDir + "/" + (forTestSources ? "test" : "production");
  }

  /**
   * @param module
   * @param forTestClasses true if directory for test sources, false - for sources.
   * @return a directory to which the sources (or test sources depending on the second partameter) should be compiled.
   * Null is returned if output directory is not specified or is not valid
   */
  @Nullable
  public static VirtualFile getModuleOutputDirectory(final Module module, boolean forTestClasses) {
    final ModuleCompilerPathsManager manager = ModuleCompilerPathsManager.getInstance(module);
    VirtualFile outPath;
    if (forTestClasses) {
      final VirtualFile path = manager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
      if (path != null) {
        outPath = path;
      }
      else {
        outPath = manager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
      }
    }
    else {
      outPath = manager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
    }
    if (outPath == null) {
      return null;
    }
    if (!outPath.isValid()) {
      LOG.info("Requested output path for module " + module.getName() + " is not valid");
      return null;
    }
    return outPath;
  }

  /**
   * The same as {@link #getModuleOutputDirectory} but returns String.
   * The method still returns a non-null value if the output path is specified in Settings but does not exist on disk.
   */
  @Nullable
  @Deprecated
  public static String getModuleOutputPath(final Module module, final boolean forTestClasses) {
    final String outPathUrl;
    final Application application = Application.get();
    final ModuleCompilerPathsManager pathsManager = ModuleCompilerPathsManager.getInstance(module);

    if (application.isDispatchThread()) {
      outPathUrl = pathsManager.getCompilerOutputUrl(forTestClasses ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance());
    }
    else {
      outPathUrl = application.runReadAction(
              (Computable<String>)() -> pathsManager.getCompilerOutputUrl(forTestClasses ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance()));
    }

    return outPathUrl != null ? VirtualFileManager.extractPath(outPathUrl) : null;
  }

  @Nullable
  public static String getModuleOutputPath(final Module module, final ContentFolderTypeProvider contentFolderType) {
    final String outPathUrl;
    final Application application = Application.get();
    final ModuleCompilerPathsManager pathsManager = ModuleCompilerPathsManager.getInstance(module);

    if (application.isDispatchThread()) {
      outPathUrl = pathsManager.getCompilerOutputUrl(contentFolderType);
    }
    else {
      outPathUrl = application.runReadAction((Computable<String>)() -> pathsManager.getCompilerOutputUrl(contentFolderType));
    }

    return outPathUrl != null ? VirtualFileManager.extractPath(outPathUrl) : null;
  }


  public static String[] getOutputPaths(Module[] modules) {
    if (modules.length == 0) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    final Set<String> outputPaths = new LinkedHashSet<>();
    for (Module module : modules) {
      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
        String outputPathUrl = ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(contentFolderType);
        if (outputPathUrl != null) {
          outputPaths.add(VirtualFileManager.extractPath(outputPathUrl).replace('/', File.separatorChar));
        }
      }
    }

    return ArrayUtil.toStringArray(outputPaths);
  }

  public static VirtualFile[] getOutputDirectories(final Module[] modules) {
    if (modules.length == 0) {
      return VirtualFile.EMPTY_ARRAY;
    }

    final Set<VirtualFile> dirs = new LinkedHashSet<>();
    for (Module module : modules) {
      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
        VirtualFile virtualFile = ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(contentFolderType);
        if (virtualFile != null) {
          dirs.add(virtualFile);
        }
      }
    }

    return VirtualFileUtil.toVirtualFileArray(dirs);
  }
}
