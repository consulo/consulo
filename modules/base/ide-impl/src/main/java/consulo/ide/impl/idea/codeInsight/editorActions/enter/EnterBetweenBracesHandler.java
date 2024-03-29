// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.editorActions.enter;

import consulo.codeEditor.Editor;
import consulo.language.editor.action.EnterBetweenBracesDelegate;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import jakarta.annotation.Nonnull;

/**
 * @deprecated Please, use the {@code EnterBetweenBracesDelegate} language-specific implementation instead.
 */
@Deprecated
public class EnterBetweenBracesHandler extends EnterBetweenBracesFinalHandler {
  @Override
  protected boolean isApplicable(@Nonnull PsiFile file, @Nonnull Editor editor, CharSequence documentText, int caretOffset, EnterBetweenBracesDelegate helper) {
    int prevCharOffset = CharArrayUtil.shiftBackward(documentText, caretOffset - 1, " \t");
    int nextCharOffset = CharArrayUtil.shiftForward(documentText, caretOffset, " \t");
    return isValidOffset(prevCharOffset, documentText) &&
           isValidOffset(nextCharOffset, documentText) &&
           isBracePair(documentText.charAt(prevCharOffset), documentText.charAt(nextCharOffset)) &&
           !ourDefaultBetweenDelegate.bracesAreInTheSameElement(file, editor, prevCharOffset, nextCharOffset);
  }

  protected boolean isBracePair(char lBrace, char rBrace) {
    return ourDefaultBetweenDelegate.isBracePair(lBrace, rBrace);
  }
}
