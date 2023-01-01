// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.generation.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.generation.surroundWith.SurroundWithHandler;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;

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
  @RequiredReadAction
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    final Language language = file.getLanguage();
    if (!SurroundDescriptor.forLanguage(language).isEmpty()) {
      return true;
    }
    final PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
    if (baseFile != null && baseFile != file && !SurroundDescriptor.forLanguage(baseFile.getLanguage()).isEmpty()) {
      return true;
    }

    if (!TemplateManager.getInstance(project).listApplicableTemplateWithInsertingDummyIdentifier(TemplateActionContext.surrounding(file, editor)).isEmpty()) {
      return true;
    }

    return false;
  }
}