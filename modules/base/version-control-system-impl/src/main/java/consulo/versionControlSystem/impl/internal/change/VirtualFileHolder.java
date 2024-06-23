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
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.versionControlSystem.change.VcsDirtyScope;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public class VirtualFileHolder implements FileHolder {
  private final Set<VirtualFile> myFiles = new HashSet<>();
  private final Project myProject;

  public VirtualFileHolder(Project project) {
    myProject = project;
  }

  @Override
  public void cleanAll() {
    myFiles.clear();
  }

  static void cleanScope(final Set<VirtualFile> files, final VcsDirtyScope scope) {
    ProgressManager.checkCanceled();
    if (files.isEmpty()) return;

    if (scope.getRecursivelyDirtyDirectories().size() == 0) {
      var dirtyFiles = scope.getDirtyFiles();
      var cleanDroppedFiles = false;

      for (var dirtyFile : dirtyFiles) {
        var f = dirtyFile.getVirtualFile();
        if (f != null) {
          files.remove(f);
        }
        else {
          cleanDroppedFiles = true;
        }
      }
      if (cleanDroppedFiles) {
        var iterator = files.iterator();
        while (iterator.hasNext()) {
          var file = iterator.next();
          if (!file.isValid()) {
            iterator.remove();
          }
        }
      }
    }
    else {
      var iterator = files.iterator();
      while (iterator.hasNext()) {
        var file = iterator.next();
        if (!file.isValid() || scope.belongsTo(VcsUtil.getFilePath(file))) {
          iterator.remove();
        }
      }
    }
  }

  @Override
  public void cleanUnderScope(@Nonnull final VcsDirtyScope scope) {
    cleanScope(myFiles, scope);
  }

  public void addFile(VirtualFile file) {
    myFiles.add(file);
  }

  public void removeFile(VirtualFile file) {
    myFiles.remove(file);
  }

  public List<VirtualFile> getFiles() {
    return new ArrayList<>(myFiles);
  }

  @Override
  public VirtualFileHolder copy() {
    final VirtualFileHolder copyHolder = new VirtualFileHolder(myProject);
    copyHolder.myFiles.addAll(myFiles);
    return copyHolder;
  }

  public boolean containsFile(final VirtualFile file) {
    return myFiles.contains(file);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final VirtualFileHolder that = (VirtualFileHolder)o;

    if (!myFiles.equals(that.myFiles)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myFiles.hashCode();
  }
}
