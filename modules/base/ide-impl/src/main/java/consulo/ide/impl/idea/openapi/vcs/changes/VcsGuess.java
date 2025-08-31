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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.ApplicationManager;
import consulo.versionControlSystem.impl.internal.ProjectLevelVcsManagerImpl;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

public class VcsGuess {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ProjectLevelVcsManagerImpl myVcsManager;

  public VcsGuess(@Nonnull Project project) {
    myProject = project;
    myVcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject);
  }

  @Nullable
  public AbstractVcs getVcsForDirty(@Nonnull VirtualFile file) {
    if (file.isInLocalFileSystem() && isFileInIndex(null, file)) {
      return myVcsManager.getVcsFor(file);
    }
    return null;
  }

  @jakarta.annotation.Nullable
  public AbstractVcs getVcsForDirty(@Nonnull FilePath filePath) {
    if (filePath.isNonLocal()) {
      return null;
    }
    VirtualFile validParent = ChangesUtil.findValidParentAccurately(filePath);
    if (validParent != null && isFileInIndex(filePath, validParent)) {
      return myVcsManager.getVcsFor(validParent);
    }
    return null;
  }

  private boolean isFileInIndex(@Nullable final FilePath filePath, @Nonnull final VirtualFile validParent) {
    return ApplicationManager.getApplication().runReadAction(new Supplier<Boolean>() {
      public Boolean get() {
        if (myProject.isDisposed()) return false;
        boolean inContent = myVcsManager.isFileInContent(validParent);
        if (inContent) return true;
        if (filePath != null) {
          return isFileInBaseDir(filePath, myProject.getBaseDir()) && !myVcsManager.isIgnored(validParent);
        }
        return false;
      }
    });
  }

  private static boolean isFileInBaseDir(@Nonnull FilePath filePath, @Nullable VirtualFile baseDir) {
    VirtualFile parent = filePath.getVirtualFileParent();
    return !filePath.isDirectory() && parent != null && parent.equals(baseDir);
  }
}
