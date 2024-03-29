// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.ui.internal;

import consulo.application.progress.DumbProgressIndicator;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.LineFragment;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows to calculate difference between two versions of document (before and after code style setting value change).
 */
public final class ChangesDiffCalculator {
  private static final Logger LOG = Logger.getInstance(ChangesDiffCalculator.class);

  public static List<TextRange> calculateDiff(@Nonnull Document beforeDocument, @Nonnull Document currentDocument) {
    CharSequence beforeText = beforeDocument.getCharsSequence();
    CharSequence currentText = currentDocument.getCharsSequence();

    try {
      ComparisonManager manager = ComparisonManager.getInstance();
      List<LineFragment> lineFragments = manager.compareLinesInner(beforeText, currentText, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);

      List<TextRange> modifiedRanges = new ArrayList<>();

      for (LineFragment lineFragment : lineFragments) {
        int fragmentStartOffset = lineFragment.getStartOffset2();
        int fragmentEndOffset = lineFragment.getEndOffset2();

        List<DiffFragment> innerFragments = lineFragment.getInnerFragments();
        if (innerFragments != null) {
          for (DiffFragment innerFragment : innerFragments) {
            int innerFragmentStartOffset = fragmentStartOffset + innerFragment.getStartOffset2();
            int innerFragmentEndOffset = fragmentStartOffset + innerFragment.getEndOffset2();
            modifiedRanges.add(calculateChangeHighlightRange(currentText, innerFragmentStartOffset, innerFragmentEndOffset));
          }
        }
        else {
          modifiedRanges.add(calculateChangeHighlightRange(currentText, fragmentStartOffset, fragmentEndOffset));
        }
      }

      return modifiedRanges;
    }
    catch (DiffTooBigException e) {
      LOG.info(e);
      return Collections.emptyList();
    }
  }

  /**
   * This method shifts changed range to the rightmost possible offset.
   * <p>
   * Thus, when comparing whitespace sequences of different length, we always highlight rightmost whitespaces
   * (while general algorithm gives no warranty on this case, and usually highlights leftmost whitespaces).
   */
  @Nonnull
  private static TextRange calculateChangeHighlightRange(@Nonnull CharSequence text, int startOffset, int endOffset) {
    if (startOffset == endOffset) {
      while (startOffset < text.length() && text.charAt(startOffset) == ' ') {
        startOffset++;
      }
      return new TextRange(startOffset, startOffset);
    }

    int originalStartOffset = startOffset;
    int originalEndOffset = endOffset;

    while (endOffset < text.length() && rangesEqual(text, originalStartOffset, originalEndOffset, startOffset + 1, endOffset + 1)) {
      startOffset++;
      endOffset++;
    }

    return new TextRange(startOffset, endOffset);
  }

  private static boolean rangesEqual(@Nonnull CharSequence text, int start1, int end1, int start2, int end2) {
    if (end1 - start1 != end2 - start2) return false;
    for (int i = start1; i < end1; i++) {
      if (text.charAt(i) != text.charAt(i - start1 + start2)) return false;
    }
    return true;
  }
}
