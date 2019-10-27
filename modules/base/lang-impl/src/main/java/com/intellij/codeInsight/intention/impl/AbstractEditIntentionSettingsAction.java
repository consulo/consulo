// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.config.IntentionActionWrapper;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

abstract class AbstractEditIntentionSettingsAction implements IntentionAction {
  private static final Logger LOG = Logger.getInstance(AbstractEditIntentionSettingsAction.class);

  @Nonnull
  final String myFamilyName;
  private final boolean myEnabled;

  protected AbstractEditIntentionSettingsAction(@Nonnull IntentionAction action) {
    myFamilyName = action.getFamilyName();
    // needed for checking errors in user written actions
    //noinspection ConstantConditions
    LOG.assertTrue(myFamilyName != null, "action " + action.getClass() + " family returned null");
    myEnabled = !(action instanceof IntentionActionWrapper) || !Comparing.equal(action.getFamilyName(), ((IntentionActionWrapper)action).getFullFamilyName());
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myEnabled;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
