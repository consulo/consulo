// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.choice;

import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction;
import com.intellij.codeInsight.intention.CustomizableIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.annotation.Nonnull;

/**
 * Intention action that is used as a title of [IntentionActionWithChoice].
 * <p>
 * Note, that this action should be non-selectable in any UI, since it does
 * not have any implementation for invoke.
 */
public class ChoiceTitleIntentionAction extends AbstractEmptyIntentionAction implements CustomizableIntentionAction, LocalQuickFix, Comparable<IntentionAction> {
  private final String myFamily;
  private final String myTitle;

  public ChoiceTitleIntentionAction(@Nonnull String family, @Nonnull String title) {
    myFamily = family;
    myTitle = title;
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return myFamily;
  }

  @Override
  public boolean isShowIcon() {
    return false;
  }

  @Override
  public boolean isSelectable() {
    return false;
  }

  @Override
  public boolean isShowSubmenu() {
    return false;
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
  }

  @Nonnull
  @Override
  public String getText() {
    return myTitle;
  }

  @Override
  public int compareTo(@Nonnull IntentionAction other) {
    if (!getFamilyName().equals(other.getFamilyName())) return getFamilyName().compareTo(other.getFamilyName());

    if (other instanceof ChoiceVariantIntentionAction){
      return -1;
    }

    return 0;
  }
}
