// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.impl.RangeMarkerImpl;
import consulo.document.internal.DocumentEx;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import org.intellij.lang.annotations.MagicConstant;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
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
  private TextAttributes myTextAttributes;
  private LineMarkerRenderer myLineMarkerRenderer;
  private ColorValue myErrorStripeColor;
  private Color myLineSeparatorColor;
  private SeparatorPlacement mySeparatorPlacement;
  private GutterIconRenderer myGutterIconRenderer;
  private Object myErrorStripeTooltip;
  private MarkupEditorFilter myFilter = MarkupEditorFilter.EMPTY;
  private CustomHighlighterRenderer myCustomRenderer;
  private LineSeparatorRenderer myLineSeparatorRenderer;

  private byte myFlags;

  private static final byte AFTER_END_OF_LINE_MASK = 1;
  private static final byte ERROR_STRIPE_IS_THIN_MASK = 2;
  private static final byte TARGET_AREA_IS_EXACT_MASK = 4;
  private static final byte IN_BATCH_CHANGE_MASK = 8;
  static final byte CHANGED_MASK = 16;
  static final byte RENDERERS_CHANGED_MASK = 32;
  static final byte FONT_STYLE_OR_COLOR_CHANGED_MASK = 64;

  @MagicConstant(intValues = {AFTER_END_OF_LINE_MASK, ERROR_STRIPE_IS_THIN_MASK, TARGET_AREA_IS_EXACT_MASK, IN_BATCH_CHANGE_MASK, CHANGED_MASK, RENDERERS_CHANGED_MASK, FONT_STYLE_OR_COLOR_CHANGED_MASK})
  private @interface FlagConstant {
  }

  @MagicConstant(flags = {CHANGED_MASK, RENDERERS_CHANGED_MASK, FONT_STYLE_OR_COLOR_CHANGED_MASK})
  private @interface ChangeStatus {
  }

  RangeHighlighterImpl(@Nonnull MarkupModelImpl model,
                       int start,
                       int end,
                       int layer,
                       @Nonnull HighlighterTargetArea target,
                       TextAttributes textAttributes,
                       boolean greedyToLeft,
                       boolean greedyToRight) {
    super((DocumentEx)model.getDocument(), start, end, false, false);
    myTextAttributes = textAttributes;
    setFlag(TARGET_AREA_IS_EXACT_MASK, target == HighlighterTargetArea.EXACT_RANGE);
    myModel = model;

    registerInTree(start, end, greedyToLeft, greedyToRight, layer);
  }

  private boolean isFlagSet(@FlagConstant byte mask) {
    return BitUtil.isSet(myFlags, mask);
  }

  private void setFlag(@FlagConstant byte mask, boolean value) {
    myFlags = BitUtil.set(myFlags, mask, value);
  }


  @Override
  public TextAttributes getTextAttributes() {
    return myTextAttributes;
  }

  @Override
  public void setTextAttributes(@Nonnull TextAttributes textAttributes) {
    boolean oldRenderedInScrollBar = isRenderedInScrollBar();
    TextAttributes old = myTextAttributes;
    myTextAttributes = textAttributes;
    if (isRenderedInScrollBar() != oldRenderedInScrollBar) {
      myModel.treeFor(this).updateRenderedFlags(this);
    }
    if (old != textAttributes && (old == TextAttributes.ERASE_MARKER || textAttributes == TextAttributes.ERASE_MARKER)) {
      fireChanged(false, true);
    }
    else if (!Comparing.equal(old, textAttributes)) {
      fireChanged(false, getFontStyle(old) != getFontStyle(textAttributes) || !Comparing.equal(getForegroundColor(old), getForegroundColor(textAttributes)));
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
      fireChanged(true, false);
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
      fireChanged(true, false);
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
      fireChanged(true, false);
    }
  }

  @Override
  public ColorValue getErrorStripeMarkColor() {
    if (myErrorStripeColor == NULL_COLOR) return null;
    if (myErrorStripeColor != null) return myErrorStripeColor;
    if (myTextAttributes != null) return myTextAttributes.getErrorStripeColor();
    return null;
  }

  @Override
  public void setErrorStripeMarkColor(ColorValue color) {
    boolean oldRenderedInScrollBar = isRenderedInScrollBar();
    if (color == null) color = NULL_COLOR;
    ColorValue old = myErrorStripeColor;
    myErrorStripeColor = color;
    if (isRenderedInScrollBar() != oldRenderedInScrollBar) {
      myModel.treeFor(this).updateRenderedFlags(this);
    }
    if (!Comparing.equal(old, color)) {
      fireChanged(false, false);
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
      fireChanged(false, false);
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
      fireChanged(false, false);
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
      fireChanged(false, false);
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
      fireChanged(false, false);
    }
  }

  @Override
  public void setEditorFilter(@Nonnull MarkupEditorFilter filter) {
    myFilter = filter;
    fireChanged(false, false);
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
      fireChanged(false, false);
    }
  }

  private void fireChanged(boolean renderersChanged, boolean fontStyleOrColorChanged) {
    if (isFlagSet(IN_BATCH_CHANGE_MASK)) {
      setFlag(CHANGED_MASK, true);
      if (renderersChanged) setFlag(RENDERERS_CHANGED_MASK, true);
      if (fontStyleOrColorChanged) setFlag(FONT_STYLE_OR_COLOR_CHANGED_MASK, true);
    }
    else {
      myModel.fireAttributesChanged(this, renderersChanged, fontStyleOrColorChanged);
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
        if (startOffset >= textLength) return textLength;
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
        if (endOffset >= textLength) return endOffset;
        return Math.min(textLength, document.getLineEndOffset(document.getLineNumber(endOffset)) + 1);
      default:
        throw new IllegalStateException(getTargetArea().toString());
    }

  }

  @ChangeStatus
  byte changeAttributesNoEvents(@Nonnull Consumer<? super RangeHighlighterEx> change) {
    assert !isFlagSet(IN_BATCH_CHANGE_MASK);
    assert !isFlagSet(CHANGED_MASK);
    setFlag(IN_BATCH_CHANGE_MASK, true);
    setFlag(RENDERERS_CHANGED_MASK, false);
    setFlag(FONT_STYLE_OR_COLOR_CHANGED_MASK, false);
    byte result = 0;
    try {
      change.accept(this);
    }
    finally {
      setFlag(IN_BATCH_CHANGE_MASK, false);
      if (isFlagSet(CHANGED_MASK)) {
        result |= CHANGED_MASK;
        if (isFlagSet(RENDERERS_CHANGED_MASK)) result |= RENDERERS_CHANGED_MASK;
        if (isFlagSet(FONT_STYLE_OR_COLOR_CHANGED_MASK)) result |= FONT_STYLE_OR_COLOR_CHANGED_MASK;
      }
      setFlag(CHANGED_MASK, false);
      setFlag(RENDERERS_CHANGED_MASK, false);
      setFlag(FONT_STYLE_OR_COLOR_CHANGED_MASK, false);
    }
    return result;
  }

  private MarkupModel getMarkupModel() {
    return myModel;
  }

  @Override
  public void setLineSeparatorRenderer(LineSeparatorRenderer renderer) {
    LineSeparatorRenderer old = myLineSeparatorRenderer;
    myLineSeparatorRenderer = renderer;
    if (!Comparing.equal(old, renderer)) {
      fireChanged(true, false);
    }
  }

  @Override
  public LineSeparatorRenderer getLineSeparatorRenderer() {
    return myLineSeparatorRenderer;
  }

  @Override
  protected void registerInTree(int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
    // we store highlighters in MarkupModel
    ((MarkupModelEx)getMarkupModel()).addRangeHighlighter(this, start, end, greedyToLeft, greedyToRight, layer);
  }

  @Override
  protected boolean unregisterInTree() {
    if (!isValid()) return false;
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
    RangeHighlighterTree.RHNode node = (RangeHighlighterTree.RHNode)(Object)myNode;
    return node == null ? -1 : node.myLayer;
  }

  @Override
  public String toString() {
    return "RangeHighlighter: (" + getStartOffset() + "," + getEndOffset() + "); layer:" + getLayer() + "; tooltip: " + getErrorStripeTooltip();
  }
}
