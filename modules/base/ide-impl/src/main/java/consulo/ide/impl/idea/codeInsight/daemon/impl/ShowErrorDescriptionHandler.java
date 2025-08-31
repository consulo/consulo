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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.codeEditor.Editor;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

public class ShowErrorDescriptionHandler implements CodeInsightActionHandler {
  private final int myWidth;
  private final boolean myRequestFocus;

  public ShowErrorDescriptionHandler(int width, boolean requestFocus) {
    myWidth = width;
    myRequestFocus = requestFocus;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
    HighlightInfoImpl info = ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(editor.getDocument(), offset, false);
    if (info != null) {
      DaemonTooltipUtil.showInfoTooltip(info, editor, editor.getCaretModel().getOffset(), myWidth, myRequestFocus, true);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
