// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.ide.impl.idea.application.options.colors.highlighting;

import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Map;

import static consulo.codeEditor.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class HighlightData {
    private final int myStartOffset;
    private int myEndOffset;
    private final TextAttributesKey myHighlightType;
    private final EditorColorKey myAdditionalColorKey;

    public HighlightData(int startOffset, TextAttributesKey highlightType, @Nullable EditorColorKey additionalColorKey) {
        myStartOffset = startOffset;
        myHighlightType = highlightType;
        myAdditionalColorKey = additionalColorKey;
    }

    public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType) {
        this(startOffset, endOffset, highlightType, null);
    }

    public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType, @Nullable EditorColorKey additionalColorKey) {
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myHighlightType = highlightType;
        myAdditionalColorKey = additionalColorKey;
    }

    public void addToCollection(@Nonnull Collection<? super HighlightData> list, boolean highlighted) {
        list.add(this);
        if (highlighted) {
            list.add(new HighlightData(getStartOffset(), getEndOffset(), BLINKING_HIGHLIGHTS_ATTRIBUTES, getAdditionalColorKey()));
        }
    }

    public void addHighlToView(final Editor view, EditorColorsScheme scheme, final Map<TextAttributesKey, LocalizeValue> displayText) {

        // XXX: Hack
        if (HighlighterColors.BAD_CHARACTER.equals(myHighlightType)) {
            return;
        }

        final TextAttributes attr = scheme.getAttributes(myHighlightType);
        if (attr != null) {
            UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                try {
                    // IDEA-53203: add ERASE_MARKER for manually defined attributes
                    RangeHighlighter erasedHighlighter = view.getMarkupModel()
                        .addRangeHighlighter(myHighlightType, myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX,
                            HighlighterTargetArea.EXACT_RANGE);
                    if (erasedHighlighter instanceof RangeHighlighterEx) {
                        ((RangeHighlighterEx) erasedHighlighter).setTextAttributes(TextAttributes.ERASE_MARKER);
                    }

                    RangeHighlighter highlighter = view.getMarkupModel()
                        .addRangeHighlighter(myHighlightType, myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX,
                            HighlighterTargetArea.EXACT_RANGE);
                    final ColorValue errorStripeColor = attr.getErrorStripeColor();
                    highlighter.setErrorStripeMarkColor(errorStripeColor);
                    final LocalizeValue tooltip = displayText.get(myHighlightType);
                    highlighter.setErrorStripeTooltip(tooltip);
                    if (highlighter instanceof RangeHighlighterEx rangeHighlighterEx) {
                        rangeHighlighterEx.setVisibleIfFolded(true);
                    }
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

    public EditorColorKey getAdditionalColorKey() {
        return myAdditionalColorKey;
    }
}
