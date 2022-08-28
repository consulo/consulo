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

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.action.BackspaceHandlerDelegate;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.action.QuoteHandler;
import consulo.language.editor.highlight.BraceMatcher;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ExtensionImpl(order = "first")
public class BackspaceHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
  protected EditorActionHandler myOriginalHandler;

  @Inject
  public BackspaceHandler() {
    super(true);
  }

  @RequiredWriteAction
  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    if (!handleBackspace(editor, caret, dataContext, false)) {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }

  protected boolean handleBackspace(Editor editor, Caret caret, DataContext dataContext, boolean toWordStart) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) return false;

    PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);

    if (file == null) return false;

    if (editor.getSelectionModel().hasSelection()) return false;

    int offset = editor.getCaretModel().getOffset() - 1;
    if (offset < 0) return false;
    CharSequence chars = editor.getDocument().getCharsSequence();
    char c = chars.charAt(offset);

    final Editor injectedEditor = TypedHandler.injectedEditorIfCharTypedIsSignificant(c, editor, file);
    final Editor originalEditor = editor;
    if (injectedEditor != editor) {
      int injectedOffset = injectedEditor.getCaretModel().getOffset();
      if (isOffsetInsideInjected(injectedEditor, injectedOffset)) {
        file = PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
        editor = injectedEditor;
        offset = injectedOffset - 1;
      }
    }

    final List<BackspaceHandlerDelegate> delegates = BackspaceHandlerDelegate.EP_NAME.getExtensionList();
    if (!toWordStart) {
      for (BackspaceHandlerDelegate delegate : delegates) {
        delegate.beforeCharDeleted(c, file, editor);
      }
    }

    FileType fileType = file.getFileType();
    final QuoteHandler quoteHandler = TypedHandler.getQuoteHandler(file, editor);

    HighlighterIterator hiterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
    boolean wasClosingQuote = quoteHandler != null && quoteHandler.isClosingQuote(hiterator, offset);

    myOriginalHandler.execute(originalEditor, caret, dataContext);

    if (!toWordStart) {
      for (BackspaceHandlerDelegate delegate : delegates) {
        if (delegate.charDeleted(c, file, editor)) {
          return true;
        }
      }
    }

    if (offset >= editor.getDocument().getTextLength()) return true;

    chars = editor.getDocument().getCharsSequence();
    if (c == '(' || c == '[' || c == '{') {
      char c1 = chars.charAt(offset);
      if (c1 != getRightChar(c)) return true;

      HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
      BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
      if (!braceMatcher.isLBraceToken(iterator, chars, fileType) && !braceMatcher.isRBraceToken(iterator, chars, fileType)) {
        return true;
      }

      int rparenOffset = BraceMatchingUtil.findRightmostRParen(iterator, (IElementType)iterator.getTokenType(), chars, fileType);
      if (rparenOffset >= 0) {
        iterator = ((EditorEx)editor).getHighlighter().createIterator(rparenOffset);
        boolean matched = BraceMatchingUtil.matchBrace(chars, fileType, iterator, false);
        if (matched) return true;
      }

      editor.getDocument().deleteString(offset, offset + 1);
    }
    else if (c == '"' || c == '\'' || c == '`') {
      char c1 = chars.charAt(offset);
      if (c1 != c) return true;
      if (wasClosingQuote) return true;

      HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(offset);
      if (quoteHandler == null || !quoteHandler.isOpeningQuote(iterator, offset)) return true;

      editor.getDocument().deleteString(offset, offset + 1);
    }

    return true;
  }

  public static char getRightChar(final char c) {
    if (c == '(') return ')';
    if (c == '[') return ']';
    if (c == '{') return '}';
    assert false;
    return c;
  }

  private static boolean isOffsetInsideInjected(Editor injectedEditor, int injectedOffset) {
    if (injectedOffset == 0 || injectedOffset >= injectedEditor.getDocument().getTextLength()) {
      return false;
    }
    PsiFile injectedFile = ((EditorWindow)injectedEditor).getInjectedFile();
    InjectedLanguageManager ilm = InjectedLanguageManager.getInstance(injectedFile.getProject());
    TextRange rangeToEdit = new TextRange(injectedOffset - 1, injectedOffset + 1);
    List<TextRange> editables = ilm.intersectWithAllEditableFragments(injectedFile, rangeToEdit);

    return editables.size() == 1 && editables.get(0).equals(rangeToEdit);
  }

  @Nullable
  public static LogicalPosition getBackspaceUnindentPosition(final PsiFile file, final Editor editor) {
    if (editor.getSelectionModel().hasSelection()) return null;

    final LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();
    if (caretPos.column == 0) {
      return null;
    }
    if (!isWhitespaceBeforeCaret(editor)) {
      return null;
    }

    // Decrease column down to indentation * n
    final int indent = CodeStyleSettingsManager.getSettings(file.getProject()).getIndentOptionsByFile(file).INDENT_SIZE;
    int column = (caretPos.column - 1) / indent * indent;
    if (column < 0) {
      column = 0;
    }
    return new LogicalPosition(caretPos.line, column);
  }

  public static void deleteToTargetPosition(@Nonnull Editor editor, @Nonnull LogicalPosition pos) {
    final int offset = editor.getCaretModel().getOffset();
    final int targetOffset = editor.logicalPositionToOffset(pos);
    editor.getSelectionModel().setSelection(targetOffset, offset);
    EditorModificationUtil.deleteSelectedText(editor);
    editor.getCaretModel().moveToLogicalPosition(pos);
  }

  public static boolean isWhitespaceBeforeCaret(Editor editor) {
    final LogicalPosition caretPos = editor.getCaretModel().getLogicalPosition();
    final CharSequence charSeq = editor.getDocument().getCharsSequence();
    // smart backspace is activated only if all characters in the check range are whitespace characters
    for (int pos = 0; pos < caretPos.column; pos++) {
      // use logicalPositionToOffset to make sure tabs are handled correctly
      final LogicalPosition checkPos = new LogicalPosition(caretPos.line, pos);
      final int offset = editor.logicalPositionToOffset(checkPos);
      if (offset < charSeq.length()) {
        final char c = charSeq.charAt(offset);
        if (c != '\t' && c != ' ' && c != '\n') {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_BACKSPACE;
  }
}
