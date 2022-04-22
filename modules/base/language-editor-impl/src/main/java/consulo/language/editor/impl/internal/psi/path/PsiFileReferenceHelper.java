/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.psi.path;

import consulo.application.util.query.Query;
import consulo.language.psi.path.FileReferenceHelper;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class PsiFileReferenceHelper extends FileReferenceHelper {

  @Override
  public PsiFileSystemItem findRoot(final Project project, @Nonnull final VirtualFile file) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile contentRootForFile = index.getSourceRootForFile(file);
    if (contentRootForFile == null) contentRootForFile = index.getContentRootForFile(file);

    if (contentRootForFile != null) {
      return PsiManager.getInstance(project).findDirectory(contentRootForFile);
    }
    return null;
  }

  @Override
  @Nonnull
  public Collection<PsiFileSystemItem> getRoots(@Nonnull final Module module) {
    return getContextsForModule(module, "", GlobalSearchScope.moduleWithDependenciesScope(module));
  }

  @Override
  @Nonnull
  public Collection<PsiFileSystemItem> getContexts(final Project project, @Nonnull final VirtualFile file) {
    final PsiFileSystemItem item = getPsiFileSystemItem(project, file);
    if (item != null) {
      final PsiFileSystemItem parent = item.getParent();
      if (parent != null) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        final VirtualFile parentFile = parent.getVirtualFile();
        assert parentFile != null;
        VirtualFile root = index.getSourceRootForFile(parentFile);
        if (root != null) {
          String path = VirtualFileUtil.getRelativePath(parentFile, root, '.');

          if (path != null) {
            final Module module = ModuleUtilCore.findModuleForFile(file, project);

            if (module != null) {
              return getContextsForModule(module, path, GlobalSearchScope.moduleWithDependenciesScope(module));
            }
          }

          // TODO: content root
        }
        return Collections.singleton(parent);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public boolean isMine(final Project project, @Nonnull final VirtualFile file) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    return index.isInSourceContent(file);
  }

  @Override
  @Nonnull
  public String trimUrl(@Nonnull String url) {
    return url.trim();
  }

  static Collection<PsiFileSystemItem> getContextsForModule(@Nonnull Module module, @Nonnull String packageName, @Nullable GlobalSearchScope scope) {
    List<PsiFileSystemItem> result = null;
    Query<VirtualFile> query = DirectoryIndex.getInstance(module.getProject()).getDirectoriesByPackageName(packageName, false);
    PsiManager manager = null;

    for (VirtualFile file : query) {
      if (scope != null && !scope.contains(file)) continue;
      if (result == null) {
        result = new ArrayList<>();
        manager = PsiManager.getInstance(module.getProject());
      }
      PsiDirectory psiDirectory = manager.findDirectory(file);
      if (psiDirectory != null) result.add(psiDirectory);
    }

    return result != null ? result : Collections.emptyList();
  }
}
