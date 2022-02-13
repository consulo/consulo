/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.codeInsight.daemon.impl;

import consulo.language.editor.rawHighlight.impl.HighlightInfoImpl;
import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import consulo.editor.Editor;
import com.intellij.openapi.editor.ex.TooltipAction;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Objects;

/**
 * from kotlin
 */
public class DaemonTooltipAction implements TooltipAction {
  private final String myFixText;
  private final int myActualOffset;

  public DaemonTooltipAction(String fixText, int actualOffset) {
    myFixText = fixText;
    myActualOffset = actualOffset;
  }

  @Nonnull
  @Override
  public String getText() {
    return myFixText;
  }

  @Override
  public void execute(@Nonnull Editor editor, @Nullable InputEvent event) {
    Project project = editor.getProject();

    if (project == null) {
      return;
    }

    // TooltipActionsLogger.logExecute(project, inputEvent);

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (psiFile == null) {
      return;
    }

    List<HighlightInfoImpl.IntentionActionDescriptor> intentions = ShowIntentionsPass.getAvailableFixes(editor, psiFile, -1, myActualOffset);

    for (HighlightInfoImpl.IntentionActionDescriptor descriptor : intentions) {
      IntentionAction action = descriptor.getAction();

      if (myFixText.equals(action.getText())) {
        //unfortunately it is very common case when quick fixes/refactorings use caret position
        editor.getCaretModel().moveToOffset(myActualOffset);

        ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, action, myFixText);
        return;
      }
    }
  }

  @Override
  public void showAllActions(@Nonnull Editor editor) {
    editor.getCaretModel().moveToOffset(myActualOffset);

    Project project = editor.getProject();

    if (project == null) {
      return;
    }

    //TooltipActionsLogger.logShowAll(project);

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (psiFile == null) {
      return;
    }

    new ShowIntentionActionsHandler().invoke(project, editor, psiFile);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DaemonTooltipAction that = (DaemonTooltipAction)o;
    return myActualOffset == that.myActualOffset && Objects.equals(myFixText, that.myFixText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFixText, myActualOffset);
  }
}
