/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.highlight;

import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public class HighlightersRecycler {
  private final MultiMap<TextRange, RangeHighlighter> incinerator = new MultiMap<>();

  public void recycleHighlighter(@Nonnull RangeHighlighter highlighter) {
    if (highlighter.isValid()) {
      incinerator.putValue(ProperTextRange.create(highlighter), highlighter);
    }
  }

  public RangeHighlighter pickupHighlighterFromGarbageBin(int startOffset, int endOffset, int layer) {
    TextRange range = new TextRange(startOffset, endOffset);
    Collection<RangeHighlighter> collection = incinerator.get(range);
    for (RangeHighlighter highlighter : collection) {
      if (highlighter.isValid() && highlighter.getLayer() == layer) {
        incinerator.remove(range, highlighter);
        return highlighter;
      }
    }
    return null;
  }

  @Nonnull
  public Collection<? extends RangeHighlighter> forAllInGarbageBin() {
    return incinerator.values();
  }
}
