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
package consulo.ide.impl.idea.openapi.module.impl.scopes;

import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * @author nik
 */
public abstract class LibraryScopeBase extends GlobalSearchScope {
  private final LinkedHashSet<VirtualFile> myEntries;
  protected final ProjectFileIndex myIndex;

  public LibraryScopeBase(Project project, VirtualFile[] classes, VirtualFile[] sources) {
    super(project);
    myIndex = ProjectRootManager.getInstance(project).getFileIndex();
    myEntries = new LinkedHashSet<VirtualFile>(classes.length + sources.length);
    Collections.addAll(myEntries, classes);
    Collections.addAll(myEntries, sources);
  }

  public boolean contains(@Nonnull VirtualFile file) {
    return myEntries.contains(getFileRoot(file));
  }

  @Nullable
  protected VirtualFile getFileRoot(VirtualFile file) {
    if (myIndex.isInLibraryClasses(file)) {
      return myIndex.getClassRootForFile(file);
    }
    if (myIndex.isInContent(file)) {
      return myIndex.getSourceRootForFile(file);
    }
    return null;
  }

  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    final VirtualFile r1 = getFileRoot(file1);
    final VirtualFile r2 = getFileRoot(file2);
    for (VirtualFile root : myEntries) {
      if (Comparing.equal(r1, root)) return 1;
      if (Comparing.equal(r2, root)) return -1;
    }
    return 0;
  }

  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return false;
  }

  public boolean isSearchInLibraries() {
    return true;
  }
}
