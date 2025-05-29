// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ApplicationManager;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.codeInsight.hints.presentation.PresentationFactory;
import consulo.language.editor.inlay.HintColorKind;
import consulo.language.editor.inlay.HintFontSize;
import consulo.language.editor.inlay.HintMarginPadding;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.Map;

/**
 * A view that renders an array of inlay presentation entries, with margins and padding.
 */
public class InlayPresentationList implements DeclarativeHintViewWithMargins {
    public InlayData model;
    private final Runnable onStateUpdated;
    private InlayPresentationEntry[] entries;
    private int[] partialWidthSums;

    private static final Map<HintMarginPadding, int[]> MARGIN_PADDING_BY_FORMAT = new EnumMap<>(HintMarginPadding.class);

    static {
        MARGIN_PADDING_BY_FORMAT.put(HintMarginPadding.OnlyPadding, new int[]{0, 7});
        MARGIN_PADDING_BY_FORMAT.put(HintMarginPadding.MarginAndSmallerPadding, new int[]{2, 6});
    }

    private static final int ARC_WIDTH = 8;
    private static final int ARC_HEIGHT = 8;
    private static final float BACKGROUND_ALPHA = 0.55f;

    /**
     * @param model          the inlay data to render
     * @param onStateUpdated callback when the inlay state tree has been updated
     */
    public InlayPresentationList(InlayData model, Runnable onStateUpdated) {
        this.model = model;
        this.onStateUpdated = onStateUpdated;
        this.entries = buildPresentationEntries(model.getTree(), model.getProviderClass());
        this.partialWidthSums = null;
    }

    private static InlayPresentationEntry[] buildPresentationEntries(TinyTree<?> tree, Class<?> providerClass) {
        return new PresentationEntryBuilder(tree, providerClass).buildPresentationEntries();
    }

    private int[] computePartialSums(InlayTextMetrics metrics) {
        int widthSoFar = 0;
        int[] sums = new int[entries.length];
        for (int i = 0; i < entries.length; i++) {
            widthSoFar += entries[i].computeWidth(metrics);
            sums[i] = widthSoFar;
        }
        return sums;
    }

    private int[] getPartialWidthSums(InlayTextMetricsStorage storage, boolean forceRecompute) {
        if (partialWidthSums == null || forceRecompute) {
            InlayTextMetrics metrics = getMetrics(storage);
            partialWidthSums = computePartialSums(metrics);
        }
        return partialWidthSums;
    }

    @Override
    public void handleLeftClick(EditorMouseEvent e,
                                Point pointInsideInlay,
                                InlayTextMetricsStorage storage,
                                boolean controlDown) {
        InlayPresentationEntry entry = findEntryByPoint(storage, pointInsideInlay);
        if (entry == null) {
            return;
        }

        entry.handleClick(e, this, controlDown);
    }

    @Override
    public void handleRightClick(EditorMouseEvent e,
                                 Point pointInsideInlay,
                                 InlayTextMetricsStorage storage) {
        DeclarativeInlayActionService service =
            ApplicationManager.getApplication().getService(DeclarativeInlayActionService.class);
        service.invokeInlayMenu(model, e,
            new RelativePoint(e.getMouseEvent().getLocationOnScreen()));
    }

    private int[] getMarginAndPadding() {
        return MARGIN_PADDING_BY_FORMAT.get(model.getHintFormat().getHorizontalMarginPadding());
    }

    @Override
    public int getMargin() {
        return getMarginAndPadding()[0];
    }

    @Override
    public int getBoxWidth(InlayTextMetricsStorage storage, boolean forceRecompute) {
        int padding = getMarginAndPadding()[1];
        return 2 * padding + getTextWidth(storage, forceRecompute);
    }

    private int getTextWidth(InlayTextMetricsStorage storage, boolean forceRecompute) {
        int[] sums = getPartialWidthSums(storage, forceRecompute);
        return sums.length > 0 ? sums[sums.length - 1] : 0;
    }

    private InlayPresentationEntry findEntryByPoint(InlayTextMetricsStorage storage, Point pointInsideInlay) {
        int[] sums = getPartialWidthSums(storage, false);
        int initialLeft = getMarginAndPadding()[1];
        int x = pointInsideInlay.x - initialLeft;
        int prev = 0;
        for (int i = 0; i < entries.length; i++) {
            int right = sums[i];
            if (x >= prev && x < right) {
                return entries[i];
            }
            prev = right;
        }
        return null;
    }

    @Override
    public LightweightHint handleHover(EditorMouseEvent e,
                                       Point pointInsideInlay,
                                       InlayTextMetricsStorage storage) {
        String tooltip = model.getTooltip();
        if (tooltip == null) {
            return null;
        }
        return new PresentationFactory(e.getEditor())
            .showTooltip(e.getMouseEvent(), tooltip);
    }

    @RequiredUIAccess
    @Override
    public void updateModel(InlayData newModel) {
        updateStateTree(newModel.getTree(), model.getTree(), (byte) 0, (byte) 0);
        this.model = newModel;
        this.entries = buildPresentationEntries(model.getTree(), model.getProviderClass());
        this.partialWidthSums = null;
        onStateUpdated.run();
    }

    private <T> void updateStateTree(TinyTree<T> treeToUpdate,
                                     TinyTree<T> fromTree,
                                     byte updateIndex,
                                     byte fromIndex) {
        byte tag = treeToUpdate.getBytePayload(updateIndex);
        byte fromTag = fromTree.getBytePayload(fromIndex);

        if ((fromTag == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG ||
            fromTag == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG) &&
            (tag == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG ||
                tag == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG ||
                tag == InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG ||
                tag == InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG)) {
            treeToUpdate.setBytePayload(fromTag, updateIndex);
        }

        fromTree.syncProcessChildren(fromIndex, updateIndex, treeToUpdate,
            (fromChild, updateChild) -> {
                updateStateTree(treeToUpdate, fromTree, updateChild, fromChild);
                return true;
            });
    }

    public void toggleTreeState(byte parentIndexToSwitch) {
        TinyTree<?> tree = model.getTree();
        byte payload = tree.getBytePayload(parentIndexToSwitch);
        if (payload == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG ||
            payload == InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG) {
            tree.setBytePayload(InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG,
                parentIndexToSwitch);
        }
        else if (payload == InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG ||
            payload == InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG) {
            tree.setBytePayload(InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG,
                parentIndexToSwitch);
        }
        else {
            throw new IllegalStateException("Unexpected payload: " + payload);
        }
        updateModel(this.model);
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay, InlayTextMetricsStorage storage) {
        return getBoxWidth(storage, false);
    }

    @Override
    public void paint(Inlay<?> inlay,
                      Graphics2D g,
                      Rectangle2D targetRegion,
                      TextAttributes textAttributes,
                      InlayTextMetricsStorage storage) {
        RealEditor editor = (RealEditor) inlay.getEditor();
        InlayTextMetrics metrics = getMetrics(storage);
        int gap = ((int) targetRegion.getHeight() < metrics.getLineHeight() + 2) ? 1 : 2;
        TextAttributes attrs = editor.getColorsScheme()
            .getAttributes(
                HintColorKind.Default == model.getHintFormat().getColorKind()
                    ? DefaultLanguageHighlighterColors.INLAY_DEFAULT
                    : model.getHintFormat().getColorKind() == HintColorKind.Parameter
                    ? DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
                    : DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND
            );

        // Draw background
        int tx = (int) targetRegion.getX();
        int ty = (int) targetRegion.getY();
        g.translate(tx, ty);
        if (model.getHintFormat().getColorKind().hasBackground()) {
            int rectHeight = ((int) targetRegion.getHeight()) - gap * 2;
            var cfg = GraphicsUtil.setupAAPainting(g);
            GraphicsUtil.paintWithAlpha(g, BACKGROUND_ALPHA);
            ColorValue colorValue = attrs.getBackgroundColor() != null
                ? attrs.getBackgroundColor()
                : textAttributes.getBackgroundColor();
            g.setColor(TargetAWT.to(colorValue));
            g.fillRoundRect(0, gap, getBoxWidth(storage, false), rectHeight, ARC_WIDTH, ARC_HEIGHT);
            cfg.restore();
        }
        g.translate(-tx, -ty);

        // Draw entries
        int xOffset = 0;
        g.translate(tx + getMarginAndPadding()[1], ty);
        for (int i = 0; i < entries.length; i++) {
            InlayPresentationEntry entry = entries[i];
            boolean hoveredWithCtrl = entry.isHoveredWithCtrl();
            TextAttributes finalAttrs = attrs;
            if (hoveredWithCtrl) {
                TextAttributes ref = editor.getColorsScheme()
                    .getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR);
                finalAttrs = attrs.clone();
                finalAttrs.setForegroundColor(ref.getForegroundColor());
            }
            entry.render(g, metrics, finalAttrs, model.isDisabled(),
                gap, (int) targetRegion.getHeight(), editor);
            int nextX = getPartialWidthSums(storage, false)[i];
            g.translate(nextX - xOffset, 0);
            xOffset = nextX;
        }
        g.translate(-(tx + getMarginAndPadding()[1]), -ty);
    }

    private InlayTextMetrics getMetrics(InlayTextMetricsStorage storage) {
        return storage.getFontMetrics(
            model.getHintFormat().getFontSize() == HintFontSize.ABitSmallerThanInEditor
        );
    }

    @TestOnly
    public InlayPresentationEntry[] getEntries() {
        return entries;
    }

    @Override
    public InlayMouseArea getMouseArea(Point pointInsideInlay,
                                       InlayTextMetricsStorage storage) {
        InlayPresentationEntry entry = findEntryByPoint(storage, pointInsideInlay);
        return entry != null ? entry.getClickArea() : null;
    }

    public InlayData getModel() {
        return model;
    }
}
