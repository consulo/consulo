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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.action.SelectWordUtil;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.ref.Ref;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl(order = "first")
public class UnSelectWordHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  public UnSelectWordHandler() {
    super(true);
  }

  @Override
  public void execute(Editor editor, DataContext dataContext) {
    Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(CommonDataKeys.PROJECT);
    Document document = editor.getDocument();
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

    if (file == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, dataContext);
      }
      return;
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();
    doAction(editor, file);
  }


  private static void doAction(final Editor editor, PsiFile file) {
    if (file instanceof PsiCompiledFile) {
      file = ((PsiCompiledFile)file).getDecompiledPsiFile();
      if (file == null) return;
    }

    if (!editor.getSelectionModel().hasSelection()) {
      return;
    }

    CharSequence text = editor.getDocument().getCharsSequence();

    int cursorOffset = editor.getCaretModel().getOffset();

    if (cursorOffset > 0 && cursorOffset < text.length() && !Character.isJavaIdentifierPart(text.charAt(cursorOffset)) && Character.isJavaIdentifierPart(text.charAt(cursorOffset - 1))) {
      cursorOffset--;
    }

    PsiElement element = file.findElementAt(cursorOffset);

    if (element instanceof PsiWhiteSpace && cursorOffset > 0) {
      PsiElement anotherElement = file.findElementAt(cursorOffset - 1);

      if (!(anotherElement instanceof PsiWhiteSpace)) {
        element = anotherElement;
      }
    }

    if (element instanceof PsiWhiteSpace) {
      PsiElement nextSibling = element.getNextSibling();
      if (nextSibling == null) {
        element = element.getParent();
        if (element == null || element instanceof PsiFile) {
          return;
        }
        nextSibling = element.getNextSibling();
        if (nextSibling == null) {
          return;
        }
      }
      element = nextSibling;
      cursorOffset = element.getTextRange().getStartOffset();
    }

    final TextRange selectionRange = new TextRange(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());

    final Ref<TextRange> maximumRange = new Ref<TextRange>();

    final int finalCursorOffset = cursorOffset;
    SelectWordUtil.processRanges(element, text, cursorOffset, editor, new Processor<TextRange>() {
      @Override
      public boolean process(TextRange range) {
        if (selectionRange.contains(range) &&
            !range.equals(selectionRange) &&
            (range.contains(finalCursorOffset) || finalCursorOffset == range.getEndOffset()) &&
            !isOffsetCollapsed(range.getStartOffset()) &&
            !isOffsetCollapsed(range.getEndOffset())) {
          if (maximumRange.get() == null || range.contains(maximumRange.get())) {
            maximumRange.set(range);
          }
        }

        return false;
      }

      private boolean isOffsetCollapsed(int offset) {
        FoldRegion region = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
        return region != null && region.getStartOffset() != offset && region.getEndOffset() != offset;
      }
    });

    TextRange range = maximumRange.get();

    if (range == null) {
      editor.getSelectionModel().setSelection(cursorOffset, cursorOffset);
    }
    else {
      editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    }
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET;
  }
}
