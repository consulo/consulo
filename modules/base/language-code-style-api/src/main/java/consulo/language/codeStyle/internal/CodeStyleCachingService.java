// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.internal;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CodeStyleCachingService {

  static CodeStyleCachingService getInstance(@Nonnull Project project) {
    return project.getInstance(CodeStyleCachingService.class);
  }

  @Nullable
  CodeStyleSettings tryGetSettings(@Nonnull PsiFile file);

  void scheduleWhenSettingsComputed(@Nonnull PsiFile file, @Nonnull Runnable runnable);

  @Nullable
  UserDataHolder getDataHolder(@Nonnull VirtualFile virtualFile);
}
