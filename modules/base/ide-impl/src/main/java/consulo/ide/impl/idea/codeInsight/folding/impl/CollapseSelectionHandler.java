/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.document.Document;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.internal.EditorFoldingInfoImpl;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
public class CollapseSelectionHandler implements CodeInsightActionHandler {
  private static final String ourPlaceHolderText = "...";
  private static final Logger LOG = Logger.getInstance(CollapseSelectionHandler.class);

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    editor.getFoldingModel().runBatchFoldingOperation(
            new Runnable() {
              @Override
              public void run() {
                EditorFoldingInfoImpl info = EditorFoldingInfoImpl.get(editor);
                FoldingModelEx foldingModel = (FoldingModelEx) editor.getFoldingModel();
                if (editor.getSelectionModel().hasSelection()) {
                  int start = editor.getSelectionModel().getSelectionStart();
                  int end = editor.getSelectionModel().getSelectionEnd();
                  if (start + 1 >= end) {
                    return;
                  }
                  Document doc = editor.getDocument();
                  if (start < end && doc.getCharsSequence().charAt(end-1) == '\n') end--;
                  FoldRegion region;
                  if ((region = FoldingUtil.findFoldRegion(editor, start, end)) != null) {
                    if (info.getPsiElement(region) == null) {
                      editor.getFoldingModel().removeFoldRegion(region);
                      info.removeRegion(region);
                    }
                  } else if (!foldingModel.intersectsRegion(start, end)) {
                    region = foldingModel.addFoldRegion(start, end, ourPlaceHolderText);
                    LOG.assertTrue(region != null, "Fold region is not created. Folding model: " + foldingModel);
                    region.setExpanded(false);
                    int offset = Math.min(start + ourPlaceHolderText.length(), doc.getTextLength());
                    editor.getCaretModel().moveToOffset(offset);
                  }
                } else {
                  FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, editor.getCaretModel().getOffset());
                  if (regions.length > 0) {
                    FoldRegion region = regions[0];
                    if (info.getPsiElement(region) == null) {
                      editor.getFoldingModel().removeFoldRegion(region);
                      info.removeRegion(region);
                    } else {
                      region.setExpanded(!region.isExpanded());
                    }
                  }
                }
              }
            }
    );
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
