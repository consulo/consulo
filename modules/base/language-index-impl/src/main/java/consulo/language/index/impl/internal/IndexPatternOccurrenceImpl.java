// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.index.impl.internal;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternOccurrence;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author yole
 */
class IndexPatternOccurrenceImpl implements IndexPatternOccurrence {
  private final PsiFile myFile;
  private final int myStartOffset;
  private final int myEndOffset;
  private final IndexPattern myPattern;
  private final List<TextRange> myAdditionalRanges;

  IndexPatternOccurrenceImpl(@Nonnull PsiFile file, int startOffset, int endOffset, @Nonnull IndexPattern pattern, @Nonnull List<TextRange> additionalRanges) {
    myFile = file;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myPattern = pattern;
    myAdditionalRanges = additionalRanges;
  }

  @Override
  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  @Override
  @Nonnull
  public TextRange getTextRange() {
    return new TextRange(myStartOffset, myEndOffset);
  }

  @Nonnull
  @Override
  public List<TextRange> getAdditionalTextRanges() {
    return myAdditionalRanges;
  }

  @Override
  @Nonnull
  public IndexPattern getPattern() {
    return myPattern;
  }

  public int hashCode() {
    return myFile.hashCode() + myStartOffset + myEndOffset + myPattern.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof IndexPatternOccurrenceImpl)) {
      return false;
    }
    IndexPatternOccurrenceImpl todoItem = (IndexPatternOccurrenceImpl)obj;
    return myFile.equals(todoItem.myFile) && myStartOffset == todoItem.myStartOffset && myEndOffset == todoItem.myEndOffset && myPattern.equals(todoItem.myPattern);
  }
}
