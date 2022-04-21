// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import consulo.language.editor.template.context.TemplateActionContext;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import consulo.language.Language;
import com.intellij.lang.LanguageSurrounders;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import javax.annotation.Nonnull;

public class SurroundWithAction extends BaseCodeInsightAction {
  public SurroundWithAction() {
    setEnabledInModalContext(true);
  }

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new SurroundWithHandler();
  }

  //@Override
  public boolean isUpdateInBackground() {
    return false;
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    final Language language = file.getLanguage();
    if (!LanguageSurrounders.INSTANCE.allForLanguage(language).isEmpty()) {
      return true;
    }
    final PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
    if (baseFile != null && baseFile != file && !LanguageSurrounders.INSTANCE.allForLanguage(baseFile.getLanguage()).isEmpty()) {
      return true;
    }

    if (!TemplateManagerImpl.listApplicableTemplateWithInsertingDummyIdentifier(TemplateActionContext.surrounding(file, editor)).isEmpty()) {
      return true;
    }

    return false;
  }
}