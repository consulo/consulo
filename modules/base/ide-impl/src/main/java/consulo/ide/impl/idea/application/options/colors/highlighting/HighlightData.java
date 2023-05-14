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

package consulo.ide.impl.idea.application.options.colors.highlighting;

import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.awt.UIUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

import static consulo.codeEditor.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class HighlightData {
  private final int myStartOffset;
  private int myEndOffset;
  private final TextAttributesKey myHighlightType;

  public HighlightData(int startOffset, TextAttributesKey highlightType) {
    myStartOffset = startOffset;
    myHighlightType = highlightType;
  }

  public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myHighlightType = highlightType;
  }

  public void addToCollection(@Nonnull Collection<HighlightData> list, boolean highlighted) {
    list.add(this);
    if (highlighted) list.add(new HighlightData(getStartOffset(), getEndOffset(), BLINKING_HIGHLIGHTS_ATTRIBUTES));
  }

  public void addHighlToView(final Editor view, EditorColorsScheme scheme, final Map<TextAttributesKey, LocalizeValue> displayText) {
    // XXX: Hack
    if (HighlighterColors.BAD_CHARACTER.equals(myHighlightType)) {
      return;
    }

    final TextAttributes attr = scheme.getAttributes(myHighlightType);
    if (attr != null) {
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
        try {
          // IDEA-53203: add ERASE_MARKER for manually defined attributes
          view.getMarkupModel().addRangeHighlighter(myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, TextAttributes.ERASE_MARKER, HighlighterTargetArea.EXACT_RANGE);
          RangeHighlighter highlighter = view.getMarkupModel().addRangeHighlighter(myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, attr, HighlighterTargetArea.EXACT_RANGE);
          final ColorValue errorStripeColor = attr.getErrorStripeColor();
          highlighter.setErrorStripeMarkColor(errorStripeColor);
          final LocalizeValue tooltip = displayText.get(myHighlightType);
          highlighter.setErrorStripeTooltip(tooltip == null ? null : tooltip.get());
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public void setEndOffset(int endOffset) {
    myEndOffset = endOffset;
  }

  public String getHighlightType() {
    return myHighlightType.getExternalName();
  }

  public TextAttributesKey getHighlightKey() {
    return myHighlightType;
  }
}
