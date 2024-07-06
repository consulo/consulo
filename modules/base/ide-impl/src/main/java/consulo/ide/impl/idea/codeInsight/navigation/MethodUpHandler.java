/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.language.editor.moveUpDown.MethodUpDownUtil;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

public class MethodUpHandler implements CodeInsightActionHandler {
  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    LookupManager.getInstance(project).hideActiveLookup();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    int caretOffset = editor.getCaretModel().getOffset();
    int caretLine = editor.getCaretModel().getLogicalPosition().line;
    int[] offsets = MethodUpDownUtil.getNavigationOffsets(file, caretOffset);
    for(int i = offsets.length - 1; i >= 0; i--){
      int offset = offsets[i];
      if (offset < caretOffset){
        int line = editor.offsetToLogicalPosition(offset).line;
        if (line < caretLine){
          editor.getCaretModel().removeSecondaryCarets();
          editor.getCaretModel().moveToOffset(offset);
          editor.getSelectionModel().removeSelection();
          editor.getScrollingModel().scrollToCaret(ScrollType.CENTER_UP);
          IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
          break;
        }
      }
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
