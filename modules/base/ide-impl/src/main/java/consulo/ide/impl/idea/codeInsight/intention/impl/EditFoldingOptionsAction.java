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

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.application.options.editor.CodeFoldingConfigurable;
import consulo.language.editor.intention.IntentionAction;
import consulo.application.ApplicationBundle;
import consulo.codeEditor.Editor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.edit.code.folding.settings", fileExtensions = "txt", categories = "Code Folding")
public class EditFoldingOptionsAction implements IntentionAction {
  @Override
  @Nonnull
  public String getText() {
    return ApplicationBundle.message("edit.code.folding.options");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return editor.getFoldingModel().isOffsetCollapsed(editor.getCaretModel().getOffset());
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    UIAccess uiAccess = UIAccess.current();
    uiAccess.give(() -> ShowSettingsUtil.getInstance().showAndSelect(project, CodeFoldingConfigurable.class));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
