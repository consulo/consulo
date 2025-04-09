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

package consulo.language.psi.include;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class FileIncludeManager {
  public static FileIncludeManager getManager(Project project) {
    return project.getInstance(FileIncludeManager.class);
  }

  public abstract VirtualFile[] getIncludedFiles(@Nonnull VirtualFile file, boolean compileTimeOnly);

  public abstract VirtualFile[] getIncludedFiles(@Nonnull VirtualFile file, boolean compileTimeOnly, boolean recursively);

  public abstract VirtualFile[] getIncludingFiles(@Nonnull VirtualFile file, boolean compileTimeOnly);

  public abstract void processIncludingFiles(PsiFile context, Predicate<? super Pair<VirtualFile, FileIncludeInfo>> processor);

  @Nullable
  public abstract PsiFileSystemItem resolveFileInclude(@Nonnull FileIncludeInfo info, @Nonnull PsiFile context);
}
