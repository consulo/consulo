// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.event;

import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;

public class CodeStyleSettingsChangeEvent {
  private
  @Nullable
  final PsiFile myPsiFile;

  public CodeStyleSettingsChangeEvent(@Nullable PsiFile psiFile) {
    myPsiFile = psiFile;
  }

  @Nullable
  public PsiFile getPsiFile() {
    return myPsiFile;
  }
}
