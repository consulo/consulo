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
package consulo.language.editor.completion.lookup;

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class TailType {

  public static int insertChar(Editor editor, int tailOffset, char c) {
    return insertChar(editor, tailOffset, c, true);
  }

  public static int insertChar(Editor editor, int tailOffset, char c, boolean overwrite) {
    Document document = editor.getDocument();
    int textLength = document.getTextLength();
    CharSequence chars = document.getCharsSequence();
    if (tailOffset == textLength || !overwrite || chars.charAt(tailOffset) != c) {
      document.insertString(tailOffset, String.valueOf(c));
    }
    return moveCaret(editor, tailOffset, 1);
  }

  protected static int moveCaret(Editor editor, int tailOffset, int delta) {
    CaretModel model = editor.getCaretModel();
    if (model.getOffset() == tailOffset) {
      model.moveToOffset(tailOffset + delta);
    }
    return tailOffset + delta;
  }

  public static final TailType UNKNOWN = new TailType() {
    @Override
    public int processTail(Editor editor, int tailOffset) {
      return tailOffset;
    }

    public String toString() {
      return "UNKNOWN";
    }

  };

  public static final TailType NONE = new TailType() {
    @Override
    public int processTail(Editor editor, int tailOffset) {
      return tailOffset;
    }

    public String toString() {
      return "NONE";
    }
  };

  public static final TailType SEMICOLON = new CharTailType(';');
  @Deprecated
  public static final TailType EXCLAMATION = new CharTailType('!');

  public static FileType getFileType(Editor editor) {
    PsiFile psiFile = getFile(editor);
    return psiFile.getFileType();
  }

  @Nonnull
  public static PsiFile getFile(Editor editor) {
    Project project = editor.getProject();
    assert project != null;
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    assert psiFile != null;
    return psiFile;
  }

  /**
   * insert a space, overtype if already present
   */
  public static final TailType SPACE = new CharTailType(' ');
  /**
   * always insert a space
   */
  public static final TailType INSERT_SPACE = new CharTailType(' ', false);
  /**
   * insert a space unless there's one at the caret position already, followed by a word
   */
  public static final TailType HUMBLE_SPACE_BEFORE_WORD = new CharTailType(' ', false) {

    @Override
    public boolean isApplicable(@Nonnull InsertionContext context) {
      CharSequence text = context.getDocument().getCharsSequence();
      int tail = context.getTailOffset();
      if (text.length() > tail + 1 && text.charAt(tail) == ' ' && Character.isLetter(text.charAt(tail + 1))) {
        return false;
      }
      return super.isApplicable(context);
    }

    @Override
    public String toString() {
      return "HUMBLE_SPACE_BEFORE_WORD";
    }
  };
  public static final TailType DOT = new CharTailType('.');

  public static final TailType CASE_COLON = new CharTailType(':');
  public static final TailType COND_EXPR_COLON = new TailType() {
    @Override
    public int processTail(Editor editor, int tailOffset) {
      Document document = editor.getDocument();
      int textLength = document.getTextLength();
      CharSequence chars = document.getCharsSequence();

      if (tailOffset < textLength - 1 && chars.charAt(tailOffset) == ' ' && chars.charAt(tailOffset + 1) == ':') {
        return moveCaret(editor, tailOffset, 2);
      }
      if (tailOffset < textLength && chars.charAt(tailOffset) == ':') {
        return moveCaret(editor, tailOffset, 1);
      }
      document.insertString(tailOffset, " : ");
      return moveCaret(editor, tailOffset, 3);
    }

    public String toString() {
      return "COND_EXPR_COLON";
    }
  };

  public static final TailType LPARENTH = new CharTailType('(');

  public abstract int processTail(Editor editor, int tailOffset);

  public static TailType createSimpleTailType(char c) {
    return new CharTailType(c);
  }

  public boolean isApplicable(@Nonnull InsertionContext context) {
    return true;
  }
}
