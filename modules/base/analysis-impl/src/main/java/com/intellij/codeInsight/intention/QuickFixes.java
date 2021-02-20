// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention;

import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuickFixes {
  public static final LocalQuickFixAndIntentionActionOnPsiElement EMPTY_FIX = new LocalQuickFixAndIntentionActionOnPsiElement(null) {
    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getText() {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getFamilyName() {
      throw new UnsupportedOperationException();
    }
  };

  public static final IntentionAndQuickFixAction EMPTY_ACTION = new IntentionAndQuickFixAction() {
    @Nonnull
    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getFamilyName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void applyFix(@Nonnull Project project, PsiFile file, @Nullable Editor editor) {
      throw new UnsupportedOperationException();
    }
  };
}