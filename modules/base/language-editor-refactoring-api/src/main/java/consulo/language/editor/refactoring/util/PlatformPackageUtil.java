/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.util;

import consulo.application.util.query.FilteredQuery;
import consulo.application.util.query.Query;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.content.DirectoryIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public class PlatformPackageUtil {

  private static final Logger LOG = Logger.getInstance(PlatformPackageUtil.class);

  @Nullable
  private static PsiDirectory getWritableModuleDirectory(@Nonnull Query<VirtualFile> vFiles,
                                                         GlobalSearchScope scope,
                                                         PsiManager manager) {
    for (VirtualFile vFile : vFiles) {
      if (!scope.contains(vFile)) continue;
      PsiDirectory directory = manager.findDirectory(vFile);
      if (directory != null && directory.isValid() && directory.isWritable()) {
        return directory;
      }
    }
    return null;
  }


  private static PsiDirectory[] getPackageDirectories(Project project, String rootPackage, GlobalSearchScope scope) {
    PsiManager manager = PsiManager.getInstance(project);

    Query<VirtualFile> query = DirectoryIndex.getInstance(scope.getProject()).getDirectoriesByPackageName(rootPackage, true);
    query = new FilteredQuery<>(query, scope::contains);

    List<PsiDirectory> directories = ContainerUtil.mapNotNull(query.findAll(), manager::findDirectory);
    return directories.toArray(new PsiDirectory[directories.size()]);
  }

  private static String getLeftPart(String packageName) {
    int index = packageName.indexOf('.');
    return index > -1 ? packageName.substring(0, index) : packageName;
  }

  private static String cutLeftPart(String packageName) {
    int index = packageName.indexOf('.');
    return index > -1 ? packageName.substring(index + 1) : "";
  }

  @Nullable
  public static PsiDirectory getDirectory(@Nullable PsiElement element) {
    if (element == null) return null;
    // handle injection and fragment editor
    PsiFile file = FileContextUtil.getContextFile(element);
    return file == null ? null : file.getContainingDirectory();
  }
}
