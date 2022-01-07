/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.roots;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.OrderedSet;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.logging.Logger;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;

import java.io.File;
import java.util.Set;

/**
 * @author VISTALL
 * @since 18:59/03.11.13
 */
public class CompilerPathsImpl extends CompilerPathsEx {
  public static final Logger LOGGER = Logger.getInstance(CompilerPathsImpl.class);

  /**
   * @param module
   * @param forTestClasses true if directory for test sources, false - for sources.
   * @return a directory to which the sources (or test sources depending on the second partameter) should be compiled.
   *         Null is returned if output directory is not specified or is not valid
   */
  @javax.annotation.Nullable
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
      LOGGER.info("Requested output path for module " + module.getName() + " is not valid");
      return null;
    }
    return outPath;
  }

  /**
   * The same as {@link #getModuleOutputDirectory} but returns String.
   * The method still returns a non-null value if the output path is specified in Settings but does not exist on disk.
   */
  @javax.annotation.Nullable
  @Deprecated
  public static String getModuleOutputPath(final Module module, final boolean forTestClasses) {
    final String outPathUrl;
    final Application application = ApplicationManager.getApplication();
    final ModuleCompilerPathsManager pathsManager = ModuleCompilerPathsManager.getInstance(module);

    if (application.isDispatchThread()) {
      outPathUrl = pathsManager.getCompilerOutputUrl(
        forTestClasses ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance());
    }
    else {
      outPathUrl = application.runReadAction(new Computable<String>() {
        @Override
        public String compute() {
          return pathsManager.getCompilerOutputUrl(
            forTestClasses ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance());
        }
      });
    }

    return outPathUrl != null ? VirtualFileManager.extractPath(outPathUrl) : null;
  }

  @javax.annotation.Nullable
  public static String getModuleOutputPath(final Module module, final ContentFolderTypeProvider contentFolderType) {
    final String outPathUrl;
    final Application application = ApplicationManager.getApplication();
    final ModuleCompilerPathsManager pathsManager = ModuleCompilerPathsManager.getInstance(module);

    if (application.isDispatchThread()) {
      outPathUrl = pathsManager.getCompilerOutputUrl(contentFolderType);
    }
    else {
      outPathUrl = application.runReadAction(new Computable<String>() {
        @Override
        public String compute() {
          return pathsManager.getCompilerOutputUrl(contentFolderType);
        }
      });
    }

    return outPathUrl != null ? VirtualFileManager.extractPath(outPathUrl) : null;
  }


  public static String[] getOutputPaths(Module[] modules) {
    if (modules.length == 0) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    final Set<String> outputPaths = new OrderedSet<String>();
    for (Module module : modules) {
      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
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

    final Set<VirtualFile> dirs = new OrderedSet<VirtualFile>();
    for (Module module : modules) {
      for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
        VirtualFile virtualFile = ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(contentFolderType);
        if (virtualFile != null) {
          dirs.add(virtualFile);
        }
      }
    }

    return VfsUtilCore.toVirtualFileArray(dirs);
  }
}
