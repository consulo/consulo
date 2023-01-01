// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;

import javax.annotation.Nonnull;

abstract class AbstractEditIntentionSettingsAction implements SyntheticIntentionAction {
  private static final Logger LOG = Logger.getInstance(AbstractEditIntentionSettingsAction.class);

  @Nonnull
  final String myText;
  private final boolean myEnabled;

  protected AbstractEditIntentionSettingsAction(@Nonnull IntentionAction action) {
    myText = action.getText();
    // needed for checking errors in user written actions
    //noinspection ConstantConditions
    LOG.assertTrue(myText != null, "action " + action.getClass() + " text returned null");
    myEnabled = true;
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
