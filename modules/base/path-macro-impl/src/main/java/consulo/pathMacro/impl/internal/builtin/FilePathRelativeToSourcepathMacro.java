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

package consulo.pathMacro.impl.internal.builtin;

import consulo.dataContext.DataContext;
import consulo.module.content.ProjectRootManager;
import consulo.pathMacro.Macro;
import consulo.pathMacro.PathMacroBundle;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;

public class FilePathRelativeToSourcepathMacro extends Macro {
  @Override
  public String getName() {
    return "FilePathRelativeToSourcepath";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.file.path.relative.to.sourcepath.root");
  }

  @Override
  public String expand(final DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return null;
    }
    VirtualFile file = dataContext.getData(VirtualFile.KEY);
    if (file == null) {
      return null;
    }
    final VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(file);
    if (sourceRoot == null) {
      return null;
    }
    return FileUtil.getRelativePath(getIOFile(sourceRoot), getIOFile(file));
  }
}
