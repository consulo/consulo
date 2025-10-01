// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public class EnableDisableIntentionAction extends AbstractEditIntentionSettingsAction {
  private final IntentionAction myAction;

  public EnableDisableIntentionAction(@Nonnull IntentionAction action) {
    super(action);
    myAction = action;
  }

  @Override
  @Nonnull
  public String getText() {
    return IntentionManagerSettings.getInstance().isEnabled(myAction)
        ? CodeInsightLocalize.disableIntentionAction(myText).get()
        : CodeInsightLocalize.enableIntentionAction(myText).get();
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    IntentionManagerSettings mySettings = IntentionManagerSettings.getInstance();
    mySettings.setEnabled(myAction, !mySettings.isEnabled(myAction));
  }

  @Override
  public String toString() {
    return getText();
  }
}
