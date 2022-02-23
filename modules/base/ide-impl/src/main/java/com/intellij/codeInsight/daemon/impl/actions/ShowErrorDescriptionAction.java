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

package com.intellij.codeInsight.daemon.impl.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import com.intellij.codeInsight.daemon.impl.ShowErrorDescriptionHandler;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import com.intellij.openapi.util.Comparing;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.accessibility.ScreenReader;

import javax.annotation.Nonnull;
import java.awt.event.KeyEvent;

public class ShowErrorDescriptionAction extends BaseCodeInsightAction implements DumbAware {
  private static int width;
  private static boolean shouldShowDescription = false;
  private static boolean descriptionShown = true;
  private boolean myRequestFocus = false;

  public ShowErrorDescriptionAction() {
    setEnabledInModalContext(true);
  }

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new ShowErrorDescriptionHandler(shouldShowDescription ? width : 0, myRequestFocus);
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(file) && isEnabledForFile(project, editor, file);
  }

  private static boolean isEnabledForFile(Project project, Editor editor, PsiFile file) {
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
    HighlightInfoImpl info =
      ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(editor.getDocument(), editor.getCaretModel().getOffset(), false);
    return info != null && info.getDescription() != null;
  }

  @Override
  public void beforeActionPerformedUpdate(@Nonnull final AnActionEvent e) {
    super.beforeActionPerformedUpdate(e);
    // The tooltip gets the focus if using a screen reader and invocation through a keyboard shortcut.
    myRequestFocus = ScreenReader.isActive() && (e.getInputEvent() instanceof KeyEvent);

    changeState();
  }

  private static void changeState() {
    if (Comparing.strEqual(ActionManagerEx.getInstanceEx().getPrevPreformedActionId(), IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) {
      shouldShowDescription = descriptionShown;
    } else {
      shouldShowDescription = false;
      descriptionShown = true;
    }
  }

  public static void rememberCurrentWidth(int currentWidth) {
    width = currentWidth;
    descriptionShown = !shouldShowDescription;
  }

}
