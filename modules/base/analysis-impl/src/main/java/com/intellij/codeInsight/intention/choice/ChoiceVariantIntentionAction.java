// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.choice;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.intention.CustomizableIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.openapi.util.Iconable;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Intention action that is used as a variant of [IntentionActionWithChoice].
 * <p>
 * Action should implement [invoke], requests to [applyFix] would proxied to [invoke].
 * <p>
 * Actions requires [index] param so it can maintain order of variants in
 * quick-fix popup.
 */
public abstract class ChoiceVariantIntentionAction extends IntentionAndQuickFixAction implements HighlightInfoType.Iconable, Iconable, CustomizableIntentionAction, Comparable<IntentionAction> {
  public abstract int getIndex();

  @Override
  public boolean isSelectable() {
    return true;
  }

  @Override
  public boolean isShowSubmenu() {
    return false;
  }

  @Override
  public boolean isShowIcon() {
    return true;
  }

  @Nullable
  @Override
  public Image getIcon() {
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  @Nullable
  @Override
  public Image getIcon(@IconFlags int flags) {
    return getIcon();
  }

  @Override
  public int compareTo(@Nonnull IntentionAction other) {
    if (!getFamilyName().equals(other.getFamilyName())) return this.getFamilyName().compareTo(other.getFamilyName());

    if (other instanceof ChoiceTitleIntentionAction) {
      return 1;
    }

    if (other instanceof ChoiceVariantIntentionAction) {
      return this.getIndex() - ((ChoiceVariantIntentionAction)other).getIndex();
    }

    return 0;
  }
}
