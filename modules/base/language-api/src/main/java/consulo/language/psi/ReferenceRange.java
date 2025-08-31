/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class ReferenceRange {
  private ReferenceRange() {
  }

  @RequiredReadAction
  public static List<TextRange> getRanges(PsiReference ref) {
    if (ref instanceof MultiRangeReference) {
      return ((MultiRangeReference)ref).getRanges();
    }
    return Collections.singletonList(ref.getRangeInElement());
  }

  @RequiredReadAction
  public static List<TextRange> getAbsoluteRanges(PsiReference ref) {
    PsiElement elt = ref.getElement();
    List<TextRange> relativeRanges = getRanges(ref);
    List<TextRange> answer = new ArrayList<TextRange>(relativeRanges.size());
    int parentOffset = elt.getTextRange().getStartOffset();
    for (TextRange relativeRange : relativeRanges) {
      answer.add(relativeRange.shiftRight(parentOffset));
    }
    return answer;
  }

  @RequiredReadAction
  public static TextRange getRange(PsiReference ref) {
    if (ref instanceof MultiRangeReference) {
      List<TextRange> ranges = ((MultiRangeReference)ref).getRanges();
      return new TextRange(ranges.get(0).getStartOffset(), ranges.get(ranges.size() - 1).getEndOffset());
    }

    return ref.getRangeInElement();
  }

  @RequiredReadAction
  public static boolean containsOffsetInElement(PsiReference ref, int offset) {
    if (ref instanceof MultiRangeReference) {
      for (TextRange range : ((MultiRangeReference)ref).getRanges()) {
        if (range.containsOffset(offset)) return true;
      }

      return false;
    }
    TextRange rangeInElement = ref.getRangeInElement();
    return rangeInElement != null && rangeInElement.containsOffset(offset);
  }

  @RequiredReadAction
  public static boolean containsRangeInElement(PsiReference ref, TextRange rangeInElement) {
    if (ref instanceof MultiRangeReference) {
      for (TextRange range : ((MultiRangeReference)ref).getRanges()) {
        if (range.contains(rangeInElement)) return true;
      }

      return false;
    }
    TextRange rangeInElement1 = ref.getRangeInElement();
    return rangeInElement1 != null && rangeInElement1.contains(rangeInElement);
  }
}
