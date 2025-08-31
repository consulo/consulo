/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class ParenthesesInsertHandler<T extends LookupElement> implements InsertHandler<T> {
  public static final ParenthesesInsertHandler<LookupElement> WITH_PARAMETERS = new ParenthesesInsertHandler<LookupElement>() {
    @Override
    protected boolean placeCaretInsideParentheses(InsertionContext context, LookupElement item) {
      return true;
    }
  };
  public static final ParenthesesInsertHandler<LookupElement> NO_PARAMETERS = new ParenthesesInsertHandler<LookupElement>() {
    @Override
    protected boolean placeCaretInsideParentheses(InsertionContext context, LookupElement item) {
      return false;
    }
  };

  public static ParenthesesInsertHandler<LookupElement> getInstance(boolean hasParameters) {
    return hasParameters ? WITH_PARAMETERS : NO_PARAMETERS;
  }

  public static ParenthesesInsertHandler<LookupElement> getInstance(final boolean hasParameters,
                                                                    final boolean spaceBeforeParentheses,
                                                                    final boolean spaceBetweenParentheses,
                                                                    final boolean insertRightParenthesis,
                                                                    boolean allowParametersOnNextLine) {
    return new ParenthesesInsertHandler<LookupElement>(spaceBeforeParentheses, spaceBetweenParentheses, insertRightParenthesis, allowParametersOnNextLine) {
      @Override
      protected boolean placeCaretInsideParentheses(InsertionContext context, LookupElement item) {
        return hasParameters;
      }
    };
  }

  private final boolean mySpaceBeforeParentheses;
  private final boolean mySpaceBetweenParentheses;
  private final boolean myMayInsertRightParenthesis;
  private final boolean myAllowParametersOnNextLine;
  private final char myLeftParenthesis;
  private final char myRightParenthesis;

  protected ParenthesesInsertHandler(boolean spaceBeforeParentheses, boolean spaceBetweenParentheses, boolean mayInsertRightParenthesis) {
    this(spaceBeforeParentheses, spaceBetweenParentheses, mayInsertRightParenthesis, false);
  }

  protected ParenthesesInsertHandler(boolean spaceBeforeParentheses,
                                     boolean spaceBetweenParentheses,
                                     boolean mayInsertRightParenthesis,
                                     boolean allowParametersOnNextLine) {
    this(spaceBeforeParentheses, spaceBetweenParentheses, mayInsertRightParenthesis, allowParametersOnNextLine, '(', ')');
  }

  protected ParenthesesInsertHandler(boolean spaceBeforeParentheses,
                                     boolean spaceBetweenParentheses,
                                     boolean mayInsertRightParenthesis,
                                     boolean allowParametersOnNextLine,
                                     char leftParenthesis,
                                     char rightParenthesis) {
    mySpaceBeforeParentheses = spaceBeforeParentheses;
    mySpaceBetweenParentheses = spaceBetweenParentheses;
    myMayInsertRightParenthesis = mayInsertRightParenthesis;
    myAllowParametersOnNextLine = allowParametersOnNextLine;
    myLeftParenthesis = leftParenthesis;
    myRightParenthesis = rightParenthesis;
  }

  protected ParenthesesInsertHandler() {
    this(false, false, true);
  }

  private static boolean isToken(@Nullable PsiElement element, String text) {
    return element != null && text.equals(element.getText());
  }

  protected abstract boolean placeCaretInsideParentheses(InsertionContext context, T item);

  @Override
  public void handleInsert(InsertionContext context, T item) {
    Editor editor = context.getEditor();
    Document document = editor.getDocument();
    context.commitDocument();
    PsiElement lParen = findExistingLeftParenthesis(context);

    char completionChar = context.getCompletionChar();
    boolean putCaretInside = completionChar == myLeftParenthesis || placeCaretInsideParentheses(context, item);

    if (completionChar == myLeftParenthesis) {
      context.setAddCompletionChar(false);
    }

    if (lParen != null) {
      int lparenthOffset = lParen.getTextRange().getStartOffset();
      if (mySpaceBeforeParentheses && lparenthOffset == context.getTailOffset()) {
        document.insertString(context.getTailOffset(), " ");
        lparenthOffset++;
      }

      if (completionChar == myLeftParenthesis || completionChar == '\t') {
        editor.getCaretModel().moveToOffset(lparenthOffset + 1);
      }
      else {
        editor.getCaretModel().moveToOffset(context.getTailOffset());
      }

      context.setTailOffset(lparenthOffset + 1);

      PsiElement list = lParen.getParent();
      PsiElement last = list.getLastChild();
      if (isToken(last, String.valueOf(myRightParenthesis))) {
        int rparenthOffset = last.getTextRange().getStartOffset();
        context.setTailOffset(rparenthOffset + 1);
        if (!putCaretInside) {
          for (int i = lparenthOffset + 1; i < rparenthOffset; i++) {
            if (!Character.isWhitespace(document.getCharsSequence().charAt(i))) {
              return;
            }
          }
          editor.getCaretModel().moveToOffset(context.getTailOffset());
        }
        else if (mySpaceBetweenParentheses && document.getCharsSequence().charAt(lparenthOffset) == ' ') {
          editor.getCaretModel().moveToOffset(lparenthOffset + 2);
        }
        else {
          editor.getCaretModel().moveToOffset(lparenthOffset + 1);
        }
        return;
      }
    }
    else {
      document.insertString(context.getTailOffset(), getSpace(mySpaceBeforeParentheses) + myLeftParenthesis + getSpace(mySpaceBetweenParentheses));
      editor.getCaretModel().moveToOffset(context.getTailOffset());
    }

    if (!myMayInsertRightParenthesis) return;

    if (context.getCompletionChar() == myLeftParenthesis) {
      //todo use BraceMatchingUtil.isPairedBracesAllowedBeforeTypeInFileType
      int tail = context.getTailOffset();
      if (tail < document.getTextLength() && StringUtil.isJavaIdentifierPart(document.getCharsSequence().charAt(tail))) {
        return;
      }
    }

    document.insertString(context.getTailOffset(), getSpace(mySpaceBetweenParentheses) + myRightParenthesis);
    if (!putCaretInside) {
      editor.getCaretModel().moveToOffset(context.getTailOffset());
    }
  }

  private static String getSpace(boolean needSpace) {
    return needSpace ? " " : "";
  }

  @Nullable
  protected PsiElement findExistingLeftParenthesis(@Nonnull InsertionContext context) {
    PsiElement element = findNextToken(context);
    return isToken(element, String.valueOf(myLeftParenthesis)) ? element : null;
  }

  @Nullable
  protected PsiElement findNextToken(@Nonnull InsertionContext context) {
    PsiFile file = context.getFile();
    PsiElement element = file.findElementAt(context.getTailOffset());
    if (element instanceof PsiWhiteSpace) {
      if (!myAllowParametersOnNextLine && element.getText().contains("\n")) {
        return null;
      }
      element = file.findElementAt(element.getTextRange().getEndOffset());
    }
    return element;
  }

}
