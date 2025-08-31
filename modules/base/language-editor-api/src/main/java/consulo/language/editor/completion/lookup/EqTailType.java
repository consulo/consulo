// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion.lookup;

import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;

public class EqTailType extends TailType {
  public static final TailType INSTANCE = new EqTailType();

  protected boolean isSpaceAroundAssignmentOperators(Editor editor, int tailOffset) {
    return CodeStyleSettingsManager.getSettings(editor.getProject()).SPACE_AROUND_ASSIGNMENT_OPERATORS;
  }

  @Override
  public int processTail(Editor editor, int tailOffset) {
    Document document = editor.getDocument();
    int textLength = document.getTextLength();
    CharSequence chars = document.getCharsSequence();
    if (tailOffset < textLength - 1 && chars.charAt(tailOffset) == ' ' && chars.charAt(tailOffset + 1) == '=') {
      return moveCaret(editor, tailOffset, 2);
    }
    if (tailOffset < textLength && chars.charAt(tailOffset) == '=') {
      return moveCaret(editor, tailOffset, 1);
    }
    if (isSpaceAroundAssignmentOperators(editor, tailOffset)) {
      document.insertString(tailOffset, " =");
      tailOffset = moveCaret(editor, tailOffset, 2);
      tailOffset = insertChar(editor, tailOffset, ' ');
    }
    else {
      document.insertString(tailOffset, "=");
      tailOffset = moveCaret(editor, tailOffset, 1);
    }
    return tailOffset;
  }
}
