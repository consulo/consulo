/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.hint.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.hint.ShowExpressionTypeHandler;
import consulo.language.Language;
import consulo.ui.ex.action.AnActionEvent;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.awt.event.KeyEvent;

public class ShowExpressionTypeAction extends BaseCodeInsightAction implements DumbAware {
  private boolean myRequestFocus = false;

  public ShowExpressionTypeAction() {
    setEnabledInModalContext(true);
  }

  @RequiredUIAccess
  @Override
  public void beforeActionPerformedUpdate(@Nonnull AnActionEvent e) {
    super.beforeActionPerformedUpdate(e);
    // The tooltip gets the focus if using a screen reader and invocation through a keyboard shortcut.
    myRequestFocus = ScreenReader.isActive() && (e.getInputEvent() instanceof KeyEvent);
  }

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new ShowExpressionTypeHandler(myRequestFocus);
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    return !ShowExpressionTypeHandler.getHandlers(project, language, file.getViewProvider().getBaseLanguage()).isEmpty();
  }
}