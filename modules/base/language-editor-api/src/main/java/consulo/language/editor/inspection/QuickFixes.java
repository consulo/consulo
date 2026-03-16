// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;

public final class QuickFixes {
  public static final LocalQuickFixAndIntentionActionOnPsiElement EMPTY_FIX = new LocalQuickFixAndIntentionActionOnPsiElement(null) {
    @Override
    public void invoke(Project project, PsiFile file, @Nullable Editor editor, PsiElement startElement, PsiElement endElement) {
      throw new UnsupportedOperationException();
    }

    
    @Override
    public LocalizeValue getText() {
      throw new UnsupportedOperationException();
    }
  };

  public static final IntentionAndQuickFixAction EMPTY_ACTION = new IntentionAndQuickFixAction() {
    
    @Override
    public LocalizeValue getName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void applyFix(Project project, PsiFile file, @Nullable Editor editor) {
      throw new UnsupportedOperationException();
    }
  };
}