/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler.impl.javaCompiler;

import com.intellij.compiler.impl.AdditionalOutputDirectoriesProvider;
import com.intellij.compiler.impl.javaCompiler.annotationProcessing.AnnotationProcessingConfiguration;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 20:24/12.06.13
 */
public class JavaAdditionalOutputDirectoriesProvider implements AdditionalOutputDirectoriesProvider {

  @NotNull
  @Override
  public String[] getOutputDirectories(@NotNull Project project, @NotNull Module module) {
    JavaCompilerConfiguration javaCompilerConfiguration = JavaCompilerConfiguration.getInstance(project);
    if (!javaCompilerConfiguration.getAnnotationProcessingConfiguration(module).isEnabled()) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    final String path = getAnnotationProcessorsGenerationPath(module);
    if (path != null) {
      return new String[] {path};
    }
    else {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }
  }

  @Nullable
  public static String getAnnotationProcessorsGenerationPath(Module module) {
    final AnnotationProcessingConfiguration config =
      JavaCompilerConfiguration.getInstance(module.getProject()).getAnnotationProcessingConfiguration(module);
    final String sourceDirName = config.getGeneratedSourcesDirectoryName(false);
    if (config.isOutputRelativeToContentRoot()) {
      final String[] roots = ModuleRootManager.getInstance(module).getContentRootUrls();
      if (roots.length == 0) {
        return null;
      }
      if (roots.length > 1) {
        Arrays.sort(roots, CompilerPaths.URLS_COMPARATOR);
      }
      return StringUtil.isEmpty(sourceDirName)
             ? VirtualFileManager.extractPath(roots[0])
             : VirtualFileManager.extractPath(roots[0]) + "/" + sourceDirName;
    }


    final String path = CompilerPaths.getModuleOutputPath(module, false);
    if (path == null) {
      return null;
    }
    return StringUtil.isEmpty(sourceDirName) ? path : path + "/" + sourceDirName;
  }
}
