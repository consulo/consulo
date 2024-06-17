/*
 * Copyright 2013-2024 consulo.io
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

import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.VcsDirtyScope;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 08-Jun-24
 * <p>
 * From kotlin
 */
public class FilePathHolderImpl implements FilePathHolder {
  private final Project project;
  private Set<FilePath> files = new HashSet<>();

  public FilePathHolderImpl(Project project) {
    this.project = project;
  }

  @Nonnull
  @Override
  public Collection<FilePath> values() {
    return files;
  }

  @Override
  public void cleanAll() {
    files.clear();
  }

  @Override
  public void cleanUnderScope(VcsDirtyScope scope) {
    cleanScope(files, scope);
  }

  @Override
  public void addFile(@Nonnull FilePath file) {
    files.add(file);
  }

  public void removeFile(FilePath filePath) {
    files.remove(filePath);
  }

  @Override
  public FileHolder copy() {
    FilePathHolderImpl holder = new FilePathHolderImpl(project);
    holder.files.addAll(files);
    return holder;
  }

  @Override
  public boolean containsFile(@Nonnull FilePath file, @Nonnull VirtualFile vcsRoot) {
    return files.contains(file);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilePathHolderImpl that = (FilePathHolderImpl)o;
    return Objects.equals(files, that.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(files);
  }

  public static void cleanScope(Set<FilePath> files, VcsDirtyScope scope) {
    ProgressManager.checkCanceled();

    if (files.isEmpty()) {
      return;
    }

    if (scope.getRecursivelyDirtyDirectories().isEmpty()) {
      // `files` set is case-sensitive depending on OS, `scope.dirtyFiles` set is always case-sensitive
      // `AbstractSet.removeAll()` chooses collection to iterate through depending on its size
      // so we explicitly iterate through `scope.dirtyFiles` here
      scope.getDirtyFiles().forEach(files::remove);
    }
    else {
      files.removeIf(scope::belongsTo);
    }
  }
}
