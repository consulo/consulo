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

package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.RelativePathCalculator;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsPathPresenter;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.function.Supplier;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class ModuleVcsPathPresenter extends VcsPathPresenter {
  private final Project myProject;
  private final Provider<ProjectFileIndex> myFileIndex;

  @Inject
  public ModuleVcsPathPresenter(final Project project, Provider<ProjectFileIndex> fileIndex) {
    myProject = project;
    myFileIndex = fileIndex;
  }

  @Nonnull
  @Override
  public String getPresentableRelativePathFor(final VirtualFile file) {
    if (file == null) return "";
    return myProject.getApplication().runReadAction((Supplier<String>)() -> {
      ProjectFileIndex fileIndex = myFileIndex.get();
      Module module = fileIndex.getModuleForFile(file, false);
      VirtualFile contentRoot = fileIndex.getContentRootForFile(file, false);
      if (module == null || contentRoot == null) return file.getPresentableUrl();
      StringBuilder result = new StringBuilder();
      result.append("[");
      result.append(module.getName());
      result.append("] ");
      result.append(contentRoot.getName());
      String relativePath = VfsUtilCore.getRelativePath(file, contentRoot, File.separatorChar);
      if (!relativePath.isEmpty()) {
        result.append(File.separatorChar);
        result.append(relativePath);
      }
      return result.toString();
    });
  }

  @Nullable
  @Override
  public String getPresentableRelativePath(@Nonnull final ContentRevision fromRevision, @Nonnull final ContentRevision toRevision) {
    // need to use parent path because the old file is already not there
    FilePath fromPath = fromRevision.getFile();
    FilePath toPath = toRevision.getFile();

    if ((fromPath.getParentPath() == null) || (toPath.getParentPath() == null)) {
      return null;
    }

    final VirtualFile oldFile = fromPath.getParentPath().getVirtualFile();
    final VirtualFile newFile = toPath.getParentPath().getVirtualFile();
    if (oldFile != null && newFile != null) {
      Module oldModule = ModuleUtilCore.findModuleForFile(oldFile, myProject);
      Module newModule = ModuleUtilCore.findModuleForFile(newFile, myProject);
      if (oldModule != newModule) {
        return getPresentableRelativePathFor(oldFile);
      }
    }
    final RelativePathCalculator calculator =
            new RelativePathCalculator(toPath.getIOFile().getAbsolutePath(), fromPath.getIOFile().getAbsolutePath());
    calculator.execute();
    final String result = calculator.getResult();
    return (result == null) ? null : result.replace("/", File.separator);
  }
}
