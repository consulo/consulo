// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

import java.util.List;

public class CleanupIntentionMenuContributor implements IntentionMenuContributor {
  @Override
  public void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
    boolean cleanup = appendCleanupCode(intentions.inspectionFixesToShow, hostFile);
    if (!cleanup) {
      appendCleanupCode(intentions.errorFixesToShow, hostFile);
    }
  }

  private static boolean appendCleanupCode(@Nonnull List<HighlightInfo.IntentionActionDescriptor> actionDescriptors, @Nonnull PsiFile file) {
    for (HighlightInfo.IntentionActionDescriptor descriptor : actionDescriptors) {
      if (descriptor.canCleanup(file)) {
        IntentionManager manager = IntentionManager.getInstance();
        actionDescriptors.add(new HighlightInfo.IntentionActionDescriptor(manager.createCleanupAllIntention(), manager.getCleanupIntentionOptions(), "Code Cleanup Options"));
        return true;
      }
    }
    return false;
  }
}
