// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.impl.RangeMarkerImpl;
import consulo.document.internal.DocumentEx;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implementation of the markup element for the editor and document.
 *
 * @author max
 */
class RangeHighlighterImpl extends RangeMarkerImpl implements RangeHighlighterEx, Supplier<RangeHighlighterEx> {
    private static final ColorValue NULL_COLOR = ColorValue.dummy("must be never called");
    private static final Key<Boolean> VISIBLE_IF_FOLDED = Key.create("visible.folded");

    private final MarkupModelImpl myModel;
    private TextAttributes myForcedTextAttributes;
    private TextAttributesKey myTextAttributesKey;

    private LineMarkerRenderer myLineMarkerRenderer;
    private ColorValue myErrorStripeColor;
    private Color myLineSeparatorColor;
    private SeparatorPlacement mySeparatorPlacement;
    private GutterIconRenderer myGutterIconRenderer;
    private Object myErrorStripeTooltip;
    private MarkupEditorFilter myFilter = MarkupEditorFilter.EMPTY;
    private CustomHighlighterRenderer myCustomRenderer;
    private LineSeparatorRenderer myLineSeparatorRenderer;

    @Mask
    private byte myFlags;

    private static final byte AFTER_END_OF_LINE_MASK = 1;
    private static final byte ERROR_STRIPE_IS_THIN_MASK = 1 << 1;
    private static final byte TARGET_AREA_IS_EXACT_MASK = 1 << 2;
    private static final byte IN_BATCH_CHANGE_MASK = 1 << 3;
    static final byte CHANGED_MASK = 1 << 4;
    static final byte RENDERERS_CHANGED_MASK = 1 << 5;
    static final byte FONT_STYLE_CHANGED_MASK = 1 << 6;
    static final byte FOREGROUND_COLOR_CHANGED_MASK = -128;

    @MagicConstant(intValues = {AFTER_END_OF_LINE_MASK, ERROR_STRIPE_IS_THIN_MASK, TARGET_AREA_IS_EXACT_MASK, IN_BATCH_CHANGE_MASK,
        CHANGED_MASK, RENDERERS_CHANGED_MASK, FONT_STYLE_CHANGED_MASK, FOREGROUND_COLOR_CHANGED_MASK})
    private @interface Flag {
    }

    @MagicConstant(flags = {AFTER_END_OF_LINE_MASK, ERROR_STRIPE_IS_THIN_MASK, TARGET_AREA_IS_EXACT_MASK, IN_BATCH_CHANGE_MASK,
        CHANGED_MASK, RENDERERS_CHANGED_MASK, FONT_STYLE_CHANGED_MASK, FOREGROUND_COLOR_CHANGED_MASK})
    private @interface Mask {
    }

    RangeHighlighterImpl(@Nonnull MarkupModelImpl model,
                         int start,
                         int end,
                         int layer,
                         @Nonnull HighlighterTargetArea target,
                         @Nullable TextAttributesKey textAttributesKey,
                         boolean greedyToLeft,
                         boolean greedyToRight) {
        super((DocumentEx) model.getDocument(), start, end, false, true);
        myTextAttributesKey = textAttributesKey;
        setFlag(TARGET_AREA_IS_EXACT_MASK, target == HighlighterTargetArea.EXACT_RANGE);
        myModel = model;

        registerInTree((DocumentEx) model.getDocument(), start, end, greedyToLeft, greedyToRight, layer);

        if (LOG.isDebugEnabled()) {
            LOG.debug("RangeHighlighterImpl: create " + this);
        }
    }

    @Override
    @Nullable
    public TextAttributes getTextAttributes(@Nullable EditorColorsScheme scheme) {
        if (myForcedTextAttributes != null) {
            return myForcedTextAttributes;
        }
        if (myTextAttributesKey == null) {
            return null;
        }

        EditorColorsScheme colorScheme = scheme == null ? EditorColorsManager.getInstance().getGlobalScheme() : scheme;
        return colorScheme.getAttributes(myTextAttributesKey);
    }

    @Override
    @Nullable
    public ColorValue getForcedErrorStripeMarkColor() {
        return myErrorStripeColor;
    }

    @Override
    public @Nullable TextAttributes getForcedTextAttributes() {
        return myForcedTextAttributes;
    }

    @Override
    public void setTextAttributes(@Nullable TextAttributes textAttributes) {
        TextAttributes old = myForcedTextAttributes;
        if (old == textAttributes) {
            return;
        }

        myForcedTextAttributes = textAttributes;

        if (old == TextAttributes.ERASE_MARKER || textAttributes == TextAttributes.ERASE_MARKER ||
            old == null && myTextAttributesKey != null) {
            fireChanged(false, true, true);
        }
        else if (!Objects.equals(old, textAttributes)) {
            fireChanged(false, getFontStyle(old) != getFontStyle(textAttributes),
                !Objects.equals(getForegroundColor(old), getForegroundColor(textAttributes)));
        }
    }

    @Override
    public void setVisibleIfFolded(boolean value) {
        putUserData(VISIBLE_IF_FOLDED, value ? Boolean.TRUE : null);
    }

    @Override
    public boolean isVisibleIfFolded() {
        return VISIBLE_IF_FOLDED.isIn(this);
    }

    private static int getFontStyle(TextAttributes textAttributes) {
        return textAttributes == null ? Font.PLAIN : textAttributes.getFontType();
    }

    private static ColorValue getForegroundColor(TextAttributes textAttributes) {
        return textAttributes == null ? null : textAttributes.getForegroundColor();
    }

    @Override
    @Nonnull
    public HighlighterTargetArea getTargetArea() {
        return isFlagSet(TARGET_AREA_IS_EXACT_MASK) ? HighlighterTargetArea.EXACT_RANGE : HighlighterTargetArea.LINES_IN_RANGE;
    }

    @Nullable
    @Override
    public TextAttributesKey getTextAttributesKey() {
        return myTextAttributesKey;
    }

    @Override
    public void setTextAttributesKey(@Nonnull TextAttributesKey textAttributesKey) {
        TextAttributesKey old = myTextAttributesKey;
        myTextAttributesKey = textAttributesKey;
        if (!textAttributesKey.equals(old)) {
            fireChanged(false, myForcedTextAttributes == null, myForcedTextAttributes == null);
        }
    }

    @Override
    public LineMarkerRenderer getLineMarkerRenderer() {
        return myLineMarkerRenderer;
    }

    @Override
    public void setLineMarkerRenderer(LineMarkerRenderer renderer) {
        boolean oldRenderedInGutter = isRenderedInGutter();
        LineMarkerRenderer old = myLineMarkerRenderer;
        myLineMarkerRenderer = renderer;

        if (isRenderedInGutter() != oldRenderedInGutter) {
            myModel.treeFor(this).updateRenderedFlags(this);
        }

        if (!Comparing.equal(old, renderer)) {
            fireChanged(true, false, false);
        }
    }

    @Override
    public CustomHighlighterRenderer getCustomRenderer() {
        return myCustomRenderer;
    }

    @Override
    public void setCustomRenderer(CustomHighlighterRenderer renderer) {
        CustomHighlighterRenderer old = myCustomRenderer;
        myCustomRenderer = renderer;
        if (!Comparing.equal(old, renderer)) {
            fireChanged(true, false, false);
        }
    }

    @Override
    public GutterIconRenderer getGutterIconRenderer() {
        return myGutterIconRenderer;
    }

    @Override
    public void setGutterIconRenderer(GutterIconRenderer renderer) {
        boolean oldRenderedInGutter = isRenderedInGutter();
        GutterMark old = myGutterIconRenderer;
        myGutterIconRenderer = renderer;
        if (isRenderedInGutter() != oldRenderedInGutter) {
            myModel.treeFor(this).updateRenderedFlags(this);
        }

        if (!Comparing.equal(old, renderer)) {
            fireChanged(true, false, false);
        }
    }

    @Override
    public ColorValue getErrorStripeMarkColor(@Nullable EditorColorsScheme scheme) {
        if (myErrorStripeColor == NULL_COLOR) {
            return null;
        }
        if (myErrorStripeColor != null) {
            return myErrorStripeColor;
        }
        if (myForcedTextAttributes != null) {
            return myForcedTextAttributes.getErrorStripeColor();
        }
        TextAttributes textAttributes = getTextAttributes(scheme);
        return textAttributes != null ? textAttributes.getErrorStripeColor() : null;
    }

    @Override
    public void setErrorStripeMarkColor(ColorValue color) {
        if (color == null) {
            color = NULL_COLOR;
        }

        ColorValue old = myErrorStripeColor;

        myErrorStripeColor = color;

        if (!Objects.equals(old, color)) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public Object getErrorStripeTooltip() {
        return myErrorStripeTooltip;
    }

    @Override
    @RequiredUIAccess
    public void setErrorStripeTooltip(Object tooltipObject) {
        UIAccess.assertIsUIThread();
        Object old = myErrorStripeTooltip;
        myErrorStripeTooltip = tooltipObject;
        if (!Comparing.equal(old, tooltipObject)) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public boolean isThinErrorStripeMark() {
        return isFlagSet(ERROR_STRIPE_IS_THIN_MASK);
    }

    @Override
    @RequiredUIAccess
    public void setThinErrorStripeMark(boolean value) {
        UIAccess.assertIsUIThread();
        boolean old = isThinErrorStripeMark();
        setFlag(ERROR_STRIPE_IS_THIN_MASK, value);
        if (old != value) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public Color getLineSeparatorColor() {
        return myLineSeparatorColor;
    }

    @Override
    public void setLineSeparatorColor(Color color) {
        Color old = myLineSeparatorColor;
        myLineSeparatorColor = color;
        if (!Comparing.equal(old, color)) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public SeparatorPlacement getLineSeparatorPlacement() {
        return mySeparatorPlacement;
    }

    @Override
    public void setLineSeparatorPlacement(@Nullable SeparatorPlacement placement) {
        SeparatorPlacement old = mySeparatorPlacement;
        mySeparatorPlacement = placement;
        if (!Comparing.equal(old, placement)) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public void setEditorFilter(@Nonnull MarkupEditorFilter filter) {
        myFilter = filter;
        fireChanged(false, false, false);
    }

    @Override
    @Nonnull
    public MarkupEditorFilter getEditorFilter() {
        return myFilter;
    }

    @Override
    public boolean isAfterEndOfLine() {
        return isFlagSet(AFTER_END_OF_LINE_MASK);
    }

    @Override
    public void setAfterEndOfLine(boolean afterEndOfLine) {
        boolean old = isAfterEndOfLine();
        setFlag(AFTER_END_OF_LINE_MASK, afterEndOfLine);
        if (old != afterEndOfLine) {
            fireChanged(false, false, false);
        }
    }

    @Override
    public void fireChanged(boolean renderersChanged, boolean fontStyleChanged, boolean foregroundColorChanged) {
        if (isFlagSet(IN_BATCH_CHANGE_MASK)) {
            // under IN_BATCH_CHANGE_MASK, do not fire events, just add flags above
            int changedFlags = CHANGED_MASK | RENDERERS_CHANGED_MASK | FONT_STYLE_CHANGED_MASK | FOREGROUND_COLOR_CHANGED_MASK;
            int value = CHANGED_MASK
                | (renderersChanged ? RENDERERS_CHANGED_MASK : 0)
                | (fontStyleChanged ? FONT_STYLE_CHANGED_MASK : 0)
                | (foregroundColorChanged ? FOREGROUND_COLOR_CHANGED_MASK : 0);
            setMask(changedFlags, value | myFlags);
        }
        else {
            myModel.fireAttributesChanged(this, renderersChanged, fontStyleChanged, foregroundColorChanged);
        }
    }

    @Override
    public int getAffectedAreaStartOffset() {
        int startOffset = getStartOffset();
        switch (getTargetArea()) {
            case EXACT_RANGE:
                return startOffset;
            case LINES_IN_RANGE:
                Document document = myModel.getDocument();
                int textLength = document.getTextLength();
                if (startOffset >= textLength) {
                    return textLength;
                }
                return document.getLineStartOffset(document.getLineNumber(startOffset));
            default:
                throw new IllegalStateException(getTargetArea().toString());
        }
    }

    @Override
    public int getAffectedAreaEndOffset() {
        int endOffset = getEndOffset();
        switch (getTargetArea()) {
            case EXACT_RANGE:
                return endOffset;
            case LINES_IN_RANGE:
                Document document = myModel.getDocument();
                int textLength = document.getTextLength();
                if (endOffset >= textLength) {
                    return endOffset;
                }
                return Math.min(textLength, document.getLineEndOffset(document.getLineNumber(endOffset)) + 1);
            default:
                throw new IllegalStateException(getTargetArea().toString());
        }

    }

    // synchronized to avoid concurrent changes
    @Mask
    synchronized byte changeAttributesNoEvents(@Nonnull Consumer<? super RangeHighlighterEx> change) {
        assert !isFlagSet(IN_BATCH_CHANGE_MASK);
        assert !isFlagSet(CHANGED_MASK);
        setMask(IN_BATCH_CHANGE_MASK | RENDERERS_CHANGED_MASK | FONT_STYLE_CHANGED_MASK | FOREGROUND_COLOR_CHANGED_MASK, IN_BATCH_CHANGE_MASK);
        byte result;
        try {
            change.accept(this);
        }
        finally {
            result = myFlags;
            setMask(IN_BATCH_CHANGE_MASK | CHANGED_MASK | RENDERERS_CHANGED_MASK | FONT_STYLE_CHANGED_MASK | FOREGROUND_COLOR_CHANGED_MASK, 0);
        }
        return result;
    }

    private boolean isFlagSet(@Flag byte mask) {
        return BitUtil.isSet(myFlags, mask);
    }

    // take one bit specified by mask from value and store it to myFlags; all other bits remain intact
    private void setFlag(@Flag byte mask, boolean value) {
        //noinspection MagicConstant
        myFlags = BitUtil.set(myFlags, mask, value);
    }

    // take bits specified by mask from value and store them to myFlags; all other bits remain intact
    private void setMask(@Mask int mask, @Mask int value) {
        //noinspection MagicConstant
        myFlags = (byte) (myFlags & ~mask | value);
    }

    private MarkupModel getMarkupModel() {
        return myModel;
    }

    @Override
    public void setLineSeparatorRenderer(LineSeparatorRenderer renderer) {
        LineSeparatorRenderer old = myLineSeparatorRenderer;
        myLineSeparatorRenderer = renderer;
        if (!Comparing.equal(old, renderer)) {
            fireChanged(true, false, false);
        }
    }

    @Override
    public LineSeparatorRenderer getLineSeparatorRenderer() {
        return myLineSeparatorRenderer;
    }

    @Override
    protected void registerInTree(@Nonnull DocumentEx document, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
        // we store highlighters in MarkupModel
        myModel.addRangeHighlighter(this, start, end, greedyToLeft, greedyToRight, layer);
    }

    @Override
    protected boolean unregisterInTree() {
        if (!isValid()) {
            return false;
        }
        // we store highlighters in MarkupModel
        getMarkupModel().removeHighlighter(this);
        return true;
    }

    @Override
    public RangeHighlighterImpl get() {
        return this;
    }

    @Override
    public int getLayer() {
        RangeHighlighterTree.RHNode node = (RangeHighlighterTree.RHNode) (Object) myNode;
        return node == null ? -1 : node.myLayer;
    }

    @Override
    public String toString() {
        return "RangeHighlighter: (" + getStartOffset() + "," + getEndOffset() + "); layer:" + getLayer() + "; tooltip: " + getErrorStripeTooltip();
    }
}
