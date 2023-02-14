// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.spellcheker.tokenizer;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.tokenizer.splitter.PlainTextTokenSplitter;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

public abstract class EscapeSequenceTokenizer<T extends PsiElement> extends Tokenizer<T> {
  private static final Key<int[]> ESCAPE_OFFSETS = Key.create("escape.tokenizer.offsets");

  @RequiredReadAction
  public static void processTextWithOffsets(PsiElement element, TokenConsumer consumer, StringBuilder unescapedText,
                                            int[] offsets, int startOffset) {
    if (element != null) {
      element.putUserData(ESCAPE_OFFSETS, offsets);
    }
    final String text = unescapedText.toString();
    consumer.consumeToken(element, text, false, startOffset, TextRange.allOf(text), PlainTextTokenSplitter.getInstance());
    if (element != null) {
      element.putUserData(ESCAPE_OFFSETS, null);
    }
  }

  @Override
  @Nonnull
  public TextRange getHighlightingRange(PsiElement element, int offset, TextRange range) {
    final int[] offsets = element.getUserData(ESCAPE_OFFSETS);
    if (offsets != null) {
      int start = offsets[range.getStartOffset()];
      int end = offsets[range.getEndOffset()];

      return new TextRange(offset + start, offset + end);
    }
    return super.getHighlightingRange(element, offset, range);
  }
}
