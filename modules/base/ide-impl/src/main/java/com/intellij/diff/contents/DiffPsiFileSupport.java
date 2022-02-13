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
package com.intellij.diff.contents;

import consulo.language.editor.highlight.impl.HighlightInfoImpl;
import consulo.language.editor.highlight.HighlightInfoFilter;
import com.intellij.codeInsight.daemon.impl.IntentionActionFilter;
import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DiffPsiFileSupport {
  public static final Key<Boolean> KEY = Key.create("Diff.DiffPsiFileSupport");

  public static class HighlightFilter implements HighlightInfoFilter {
    @Override
    public boolean accept(@Nonnull HighlightInfoImpl info, @Nullable PsiFile file) {
      if (!isDiffFile(file)) return true;
      if (info.getSeverity() == HighlightSeverity.ERROR) return false;
      return true;
    }
  }

  public static class IntentionFilter implements IntentionActionFilter {
    @Override
    public boolean accept(@Nonnull IntentionAction intentionAction, @Nullable PsiFile file) {
      return !isDiffFile(file);
    }
  }

  public static class HighlightingSettingProvider extends DefaultHighlightingSettingProvider {
    @Nullable
    @Override
    public FileHighlightingSetting getDefaultSetting(@Nonnull Project project, @Nonnull VirtualFile file) {
      if (!isDiffFile(file)) return null;
      return FileHighlightingSetting.SKIP_INSPECTION;
    }
  }


  public static boolean isDiffFile(@Nullable PsiFile file) {
    return file != null && isDiffFile(file.getVirtualFile());
  }

  public static boolean isDiffFile(@Nullable VirtualFile file) {
    return file != null && file.getUserData(KEY) == Boolean.TRUE;
  }
}
