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

package consulo.ide.impl.idea.codeInsight.folding.impl.actions;

import consulo.language.editor.folding.CodeFoldingManager;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.action.EditorAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ExpandRegionAction extends EditorAction {
  public ExpandRegionAction() {
    super(new BaseFoldingHandler() {
      @Override
      public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        expandRegionAtCaret(editor.getProject(), editor);
      }
    });
  }

  private static void expandRegionAtCaret(final Project project, @Nullable final Editor editor) {
    if (editor == null) return;

    expandRegionAtOffset(project, editor, editor.getCaretModel().getOffset());
  }

  public static void expandRegionAtOffset(@Nonnull Project project, @Nonnull final Editor editor, final int offset) {
    CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);
    foldingManager.updateFoldRegions(editor);

    final int line = editor.getDocument().getLineNumber(offset);
    Runnable processor = () -> {
      FoldRegion region = FoldingUtil.findFoldRegionStartingAtLine(editor, line);
      if (region != null && !region.isExpanded()) {
        region.setExpanded(true);
      }
      else {
        FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, offset);
        for (int i = regions.length - 1; i >= 0; i--) {
          region = regions[i];
          if (!region.isExpanded()) {
            region.setExpanded(true);
            break;
          }
        }
      }
    };
    editor.getFoldingModel().runBatchFoldingOperation(processor);
  }
}
