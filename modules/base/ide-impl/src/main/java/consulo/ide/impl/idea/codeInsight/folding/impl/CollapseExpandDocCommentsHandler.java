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
package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.internal.EditorFoldingInfoImpl;
import consulo.language.psi.PsiDocCommentBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CollapseExpandDocCommentsHandler implements CodeInsightActionHandler {

  private static final Key<Boolean> DOC_COMMENT_MARK = Key.create("explicit.fold.region.doc.comment.mark");

  public static void setDocCommentMark(@Nonnull FoldRegion region, boolean value) {
    region.putUserData(DOC_COMMENT_MARK, value);
  }

  private final boolean myExpand;

  public CollapseExpandDocCommentsHandler(boolean isExpand) {
    myExpand = isExpand;
  }

  @Override
  public void invoke(@Nonnull Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);
    foldingManager.updateFoldRegions(editor);
    final FoldRegion[] allFoldRegions = editor.getFoldingModel().getAllFoldRegions();
    Runnable processor = () -> {
      for (FoldRegion region : allFoldRegions) {
        PsiElement element = EditorFoldingInfoImpl.get(editor).getPsiElement(region);
        if (element instanceof PsiDocCommentBase || Boolean.TRUE.equals(region.getUserData(DOC_COMMENT_MARK)) || hasAllowedTokenType(editor, region, element)) {
          region.setExpanded(myExpand);
        }
      }
    };
    editor.getFoldingModel().runBatchFoldingOperation(processor);
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@Nonnull PsiFile currentFile) {
    return null;
  }

  /**
   * Check if specified psi element has allowed token type as documentation.
   * <p>
   * The check is needed for languages which differentiate comments and documentation
   * and cannot use PsiComment for docs (like Python).
   */
  private static boolean hasAllowedTokenType(@Nonnull Editor editor, @Nonnull FoldRegion region, @Nullable PsiElement element) {
    if (element == null) return false;
    final Commenter commenter = Commenter.forLanguage(element.getLanguage());
    if (!(commenter instanceof CodeDocumentationAwareCommenter)) return false;
    final HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(region.getStartOffset());
    if (iterator.atEnd()) return false;
    return ((CodeDocumentationAwareCommenter)commenter).getDocumentationCommentTokenType() == iterator.getTokenType();
  }
}
