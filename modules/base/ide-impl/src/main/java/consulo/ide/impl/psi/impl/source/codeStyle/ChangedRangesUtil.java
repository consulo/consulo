// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.ChangedRangesInfo;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ChangedRangesUtil {

  @Contract(pure = true)
  private ChangedRangesUtil() {
  }

  @Nonnull
  static List<TextRange> processChangedRanges(@Nonnull PsiFile file, @Nonnull ChangedRangesInfo changedRangesInfo) {
    Document document = file.getViewProvider().getDocument();
    List<TextRange> result = new ArrayList<>();
    if (document != null) {
      FormattingRangesExtender extender = new FormattingRangesExtender(document, file);
      for (TextRange range : changedRangesInfo.allChangedRanges) {
        List<TextRange> extended = extender.getExtendedRanges(Collections.singletonList(range));
        result.addAll(extended);
      }
    }
    else {
      result.addAll(changedRangesInfo.allChangedRanges);
    }
    return optimizedChangedRanges(result);
  }

  @Nonnull
  private static List<TextRange> optimizedChangedRanges(@Nonnull List<TextRange> allChangedRanges) {
    if (allChangedRanges.isEmpty()) return allChangedRanges;
    List<TextRange> sorted = ContainerUtil.sorted(allChangedRanges, Segment.BY_START_OFFSET_THEN_END_OFFSET);

    List<TextRange> result = ContainerUtil.newSmartList();

    TextRange prev = sorted.get(0);
    for (TextRange next : sorted) {
      if (next.getStartOffset() <= prev.getEndOffset()) {
        int newEndOffset = Math.max(prev.getEndOffset(), next.getEndOffset());
        prev = new TextRange(prev.getStartOffset(), newEndOffset);
      }
      else {
        result.add(prev);
        prev = next;
      }
    }
    result.add(prev);

    return result;
  }
}
