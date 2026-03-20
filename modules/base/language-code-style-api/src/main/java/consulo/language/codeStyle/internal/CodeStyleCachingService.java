// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public interface CodeStyleCachingService {

  static CodeStyleCachingService getInstance(Project project) {
    return project.getInstance(CodeStyleCachingService.class);
  }

  @Nullable CodeStyleSettings tryGetSettings(PsiFile file);

  void scheduleWhenSettingsComputed(PsiFile file, Runnable runnable);

  @Nullable UserDataHolder getDataHolder(VirtualFile virtualFile);
}
