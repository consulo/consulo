// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternOccurrence;
import org.jspecify.annotations.Nullable;

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

    IndexPatternOccurrenceImpl(PsiFile file, int startOffset, int endOffset, IndexPattern pattern, List<TextRange> additionalRanges) {
        myFile = file;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myPattern = pattern;
        myAdditionalRanges = additionalRanges;
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    public TextRange getTextRange() {
        return new TextRange(myStartOffset, myEndOffset);
    }

    @Override
    public List<TextRange> getAdditionalTextRanges() {
        return myAdditionalRanges;
    }

    @Override
    public IndexPattern getPattern() {
        return myPattern;
    }

    @Override
    public int hashCode() {
        return myFile.hashCode() + myStartOffset + myEndOffset + myPattern.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof IndexPatternOccurrenceImpl that
            && myFile.equals(that.myFile)
            && myStartOffset == that.myStartOffset
            && myEndOffset == that.myEndOffset
            && myPattern.equals(that.myPattern);
    }
}
