// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.IntentionManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.util.List;

@ExtensionImpl(id = "cleanup", order = "after gutter")
public class CleanupIntentionMenuContributor implements IntentionMenuContributor {
  @Override
  public void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
    boolean cleanup = appendCleanupCode(intentions.inspectionFixesToShow, hostFile);
    if (!cleanup) {
      appendCleanupCode(intentions.errorFixesToShow, hostFile);
    }
  }

  private static boolean appendCleanupCode(@Nonnull List<IntentionActionDescriptor> actionDescriptors, @Nonnull PsiFile file) {
    for (IntentionActionDescriptor descriptor : actionDescriptors) {
      if (descriptor.canCleanup(file)) {
        IntentionManager manager = IntentionManager.getInstance();
        actionDescriptors.add(new IntentionActionDescriptor(manager.createCleanupAllIntention(), manager.getCleanupIntentionOptions(), "Code Cleanup Options"));
        return true;
      }
    }
    return false;
  }
}
