// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.folding.impl.actions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUtil;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.folding.CollapseBlockHandler;
import consulo.language.editor.folding.EditorFoldingInfo;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.internal.EditorFoldingInfoImpl;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;

import javax.annotation.Nonnull;
import java.util.List;

public class CollapseBlockAction extends BaseCodeInsightAction {
  private static final Logger LOG = Logger.getInstance(CollapseBlockAction.class);

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return (project, editor, file) -> executor(project, editor, file, true);
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return executor(project, editor, file, false);
  }

  private static boolean executor(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file, boolean executeAction) {
    final InjectedLanguageManager instance = InjectedLanguageManager.getInstance(project);
    while (true) {
      final List<CollapseBlockHandler> handlers = CollapseBlockHandler.forLanguage(file.getLanguage());
      if (handlers.isEmpty()) {
        if (!instance.isInjectedFragment(file) || !(editor instanceof EditorWindow)) {
          return false;
        }
        file = instance.getTopLevelFile(file);
        if (file == null) {
          return false;
        }
        editor = ((EditorWindow)editor).getDelegate();
        continue;
      }
      if (executeAction) {
        final Editor finalEditor = editor;
        final PsiFile finalFile = file;
        handlers.forEach(handler -> invoke(handler, finalEditor, finalFile));
      }
      return true;
    }
  }

  private static void invoke(@Nonnull CollapseBlockHandler collapseBlockHandler, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
    int[] targetCaretOffset = {-1};
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      final EditorFoldingInfoImpl info = (EditorFoldingInfoImpl)EditorFoldingInfo.get(editor);
      FoldingModelEx model = (FoldingModelEx)editor.getFoldingModel();
      int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset - 1);
      if (!collapseBlockHandler.isEndBlockToken(element)) {
        element = file.findElementAt(offset);
      }
      if (element == null) return;
      PsiElement block = collapseBlockHandler.findParentBlock(element);
      FoldRegion previous = null;
      FoldRegion myPrevious = null;
      while (block != null) {
        TextRange range = collapseBlockHandler.getFoldingRange(block);
        if (!range.containsOffset(offset)) {
          block = collapseBlockHandler.findParentBlock(block);
          continue;
        }
        int start = range.getStartOffset();
        int end = range.getEndOffset();
        FoldRegion existing = FoldingUtil.findFoldRegion(editor, start, end);
        if (existing != null) {
          if (existing.isExpanded()) {
            existing.setExpanded(false);
            targetCaretOffset[0] = existing.getEndOffset();
            return;
          }
          previous = existing;
          if (info.getPsiElement(existing) == null) myPrevious = existing;
          block = collapseBlockHandler.findParentBlock(block);
          continue;
        }
        if (!model.intersectsRegion(start, end)) {
          FoldRegion region = model.addFoldRegion(start, end, collapseBlockHandler.getPlaceholderText());
          LOG.assertTrue(region != null);
          region.setExpanded(false);
          if (myPrevious != null && info.getPsiElement(region) == null) {
            info.removeRegion(myPrevious);
            model.removeFoldRegion(myPrevious);
          }
          targetCaretOffset[0] = block.getTextRange().getEndOffset() < offset ? start : end;
          return;
        }
        else break;
      }
      if (previous != null) {
        previous.setExpanded(false);
        if (myPrevious != null) {
          info.removeRegion(myPrevious);
          model.removeFoldRegion(myPrevious);
        }
        targetCaretOffset[0] = previous.getEndOffset();
      }
    });
    if (targetCaretOffset[0] >= 0) editor.getCaretModel().moveToOffset(targetCaretOffset[0]);
  }
}
