// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.codeEditor.markup;

import consulo.colorScheme.TextAttributes;
import consulo.document.internal.RangeMarkerEx;
import consulo.ui.color.ColorValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;

public interface RangeHighlighterEx extends RangeHighlighter, RangeMarkerEx {
    RangeHighlighterEx[] EMPTY_ARRAY = new RangeHighlighterEx[0];

    Comparator<RangeHighlighterEx> BY_AFFECTED_START_OFFSET = Comparator.comparingInt(RangeHighlighterEx::getAffectedAreaStartOffset);

    @Nullable
    default TextAttributes getForcedTextAttributes() {
        return null;
    }

    @Nullable
    default ColorValue getForcedErrorStripeMarkColor() {
        return null;
    }

    default void fireChanged(boolean renderersChanged, boolean fontStyleChanged, boolean foregroundColorChanged) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets text attributes used for highlighting.
     * Manually set attributes have priority over {@link #getTextAttributesKey()}
     * during the calculation of {@link #getTextAttributes(EditorColorsScheme)}
     * <p>
     * Can be also used to temporary hide the highlighter
     * {@link TextAttributes#ERASE_MARKER }
     */
    void setTextAttributes(@Nullable TextAttributes textAttributes);

    default void copyFrom(@Nonnull RangeHighlighterEx other) {
        setAfterEndOfLine(other.isAfterEndOfLine());
        setGreedyToLeft(other.isGreedyToLeft());
        setGreedyToRight(other.isGreedyToRight());
        setVisibleIfFolded(other.isVisibleIfFolded());

        if (other.getForcedTextAttributes() != null) {
            setTextAttributes(other.getForcedTextAttributes());
        }
        if (other.getTextAttributesKey() != null) {
            setTextAttributesKey(other.getTextAttributesKey());
        }

        setLineMarkerRenderer(other.getLineMarkerRenderer());
        setCustomRenderer(other.getCustomRenderer());
        setGutterIconRenderer(other.getGutterIconRenderer());

        setErrorStripeMarkColor(other.getForcedErrorStripeMarkColor());
        setErrorStripeTooltip(other.getErrorStripeTooltip());
        setThinErrorStripeMark(other.isThinErrorStripeMark());

        setLineSeparatorColor(other.getLineSeparatorColor());
        setLineSeparatorPlacement(other.getLineSeparatorPlacement());
        setLineSeparatorRenderer(other.getLineSeparatorRenderer());

        setEditorFilter(other.getEditorFilter());
    }

    /**
     * If {@code true}, this highlighter is persistent and is retained between code analyzer runs and IDE restarts.
     *
     * @see MarkupModelEx#addPersistentLineHighlighter(consulo.colorScheme.TextAttributesKey, int, int)
     * @see MarkupModelEx#addRangeHighlighterAndChangeAttributes(consulo.colorScheme.TextAttributesKey, int, int, int, HighlighterTargetArea, boolean, java.util.function.Consumer)
     */
    default boolean isPersistent() {
        return false;
    }
}
