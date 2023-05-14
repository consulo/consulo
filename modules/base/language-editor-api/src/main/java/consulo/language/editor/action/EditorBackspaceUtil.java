/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public final class EditorBackspaceUtil {
  private EditorBackspaceUtil() {
  }

  public static char getRightChar(final char c) {
    if (c == '(') return ')';
    if (c == '[') return ']';
    if (c == '{') return '}';
    assert false;
    return c;
  }

  public static boolean isOffsetInsideInjected(Editor injectedEditor, int injectedOffset) {
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
    final int indent = CodeStyle.getIndentOptions(file).INDENT_SIZE;
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
}
