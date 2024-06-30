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
package consulo.ide.impl.idea.ide.macro;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerPaths;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.dataContext.DataContext;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.pathMacro.Macro;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import java.io.File;

@ExtensionImpl
public final class OutputPathMacro extends Macro {
  @Override
  public String getName() {
    return "OutputPath";
  }

  @Override
  public String getDescription() {
    return IdeLocalize.macroOutputPath().get();
  }

  @Override
  @RequiredReadAction
  public String expand(DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return null;
    }

    VirtualFile file = dataContext.getData(VirtualFile.KEY);
    if (file != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      Module module = projectFileIndex.getModuleForFile(file);
      if (module != null) {
        boolean isTest = projectFileIndex.isInTestSourceContent(file);
        String outputPathUrl = ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(isTest ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance());
        if (outputPathUrl == null) {
          return null;
        }
        return VirtualFileManager.extractPath(outputPathUrl).replace('/', File.separatorChar);
      }
    }

    Module[] allModules = ModuleManager.getInstance(project).getSortedModules();
    if (allModules.length == 0) {
      return null;
    }
    String[] paths = CompilerPaths.getOutputPaths(allModules);
    final StringBuilder outputPath = new StringBuilder();
    for (int idx = 0; idx < paths.length; idx++) {
      String path = paths[idx];
      if (idx > 0) {
        outputPath.append(File.pathSeparator);
      }
      outputPath.append(path);
    }
    return outputPath.toString();
  }
}
