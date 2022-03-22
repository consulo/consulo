/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.intention.impl;

import com.intellij.application.options.editor.CodeFoldingConfigurable;
import consulo.language.editor.intention.IntentionAction;
import consulo.application.ApplicationBundle;
import consulo.codeEditor.Editor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

/**
 * @author cdr
 */
public class EditFoldingOptionsAction implements IntentionAction {
  @Override
  @Nonnull
  public String getText() {
    return ApplicationBundle.message("edit.code.folding.options");
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return editor.getFoldingModel().isOffsetCollapsed(editor.getCaretModel().getOffset());
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    ShowSettingsUtil.getInstance().editConfigurable(project, new CodeFoldingConfigurable());
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
