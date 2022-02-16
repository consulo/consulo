// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import consulo.language.editor.intention.IntentionManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.rawHighlight.impl.HighlightInfoImpl;
import consulo.language.psi.PsiFile;
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

  private static boolean appendCleanupCode(@Nonnull List<HighlightInfoImpl.IntentionActionDescriptor> actionDescriptors, @Nonnull PsiFile file) {
    for (HighlightInfoImpl.IntentionActionDescriptor descriptor : actionDescriptors) {
      if (descriptor.canCleanup(file)) {
        IntentionManager manager = IntentionManager.getInstance();
        actionDescriptors.add(new HighlightInfoImpl.IntentionActionDescriptor(manager.createCleanupAllIntention(), manager.getCleanupIntentionOptions(), "Code Cleanup Options"));
        return true;
      }
    }
    return false;
  }
}
