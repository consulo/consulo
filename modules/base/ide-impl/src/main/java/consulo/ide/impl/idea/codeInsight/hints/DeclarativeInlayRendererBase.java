// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DeclarativeInlayRendererBase<M> implements EditorCustomElementRenderer {
    private final String providerId;
    private final String sourceId;
    private final InlayTextMetricsStorage fontMetricsStorage;
    private Inlay<? extends DeclarativeInlayRendererBase<M>> inlay;

    protected DeclarativeInlayRendererBase(String providerId,
                                           String sourceId,
                                           InlayTextMetricsStorage fontMetricsStorage) {
        this.providerId = providerId;
        this.sourceId = sourceId;
        this.fontMetricsStorage = fontMetricsStorage;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public InlayTextMetricsStorage getFontMetricsStorage() {
        return fontMetricsStorage;
    }

    public void initInlay(Inlay<? extends DeclarativeInlayRendererBase<M>> inlay) {
        this.inlay = inlay;
    }

    protected abstract DeclarativeHintView<M> getView();

    public abstract List<InlayPresentationList> getPresentationLists();

    @RequiredUIAccess
    public void updateModel(M newModel) {
        getView().updateModel(newModel);
    }

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        return getView().calcWidthInPixels(inlay, fontMetricsStorage);
    }

    @Override
    public void paint(Inlay<?> inlay,
                      Graphics2D g,
                      Rectangle2D targetRegion,
                      TextAttributes textAttributes) {
        getView().paint(inlay, g, targetRegion, textAttributes, fontMetricsStorage);
    }

    public void handleLeftClick(EditorMouseEvent e, Point pointInsideInlay, boolean controlDown) {
        getView().handleLeftClick(e, pointInsideInlay, fontMetricsStorage, controlDown);
    }

    public LightweightHint handleHover(EditorMouseEvent e, Point pointInsideInlay) {
        return getView().handleHover(e, pointInsideInlay, fontMetricsStorage);
    }

    public void handleRightClick(EditorMouseEvent e, Point pointInsideInlay) {
        getView().handleRightClick(e, pointInsideInlay, fontMetricsStorage);
    }

    public InlayMouseArea getMouseArea(Point pointInsideInlay) {
        return getView().getMouseArea(pointInsideInlay, fontMetricsStorage);
    }

    @Override
    public String getContextMenuGroupId(Inlay inlay) {
        return "DummyActionGroup";
    }

    public List<InlayData> toInlayData(boolean needUpToDateOffsets) {
        if (needUpToDateOffsets && inlay != null) {
            return getPresentationLists().stream()
                .map(list -> copyAndUpdatePosition(list.getModel(), inlay))
                .collect(Collectors.toList());
        }
        else {
            return getPresentationLists().stream()
                .map(InlayPresentationList::getModel)
                .collect(Collectors.toList());
        }
    }

    public List<InlayData> toInlayData() {
        return toInlayData(true);
    }

    private static DeclarativeInlayPosition copyAndUpdateOffset(DeclarativeInlayPosition position, Inlay<?> inlay) {
        int newOffset = inlay.getOffset();
        if (position instanceof DeclarativeInlayPosition.AboveLineIndentedPosition) {
            DeclarativeInlayPosition.AboveLineIndentedPosition orig = (DeclarativeInlayPosition.AboveLineIndentedPosition) position;
            return new DeclarativeInlayPosition.AboveLineIndentedPosition(newOffset, orig.getVerticalPriority(), orig.getPriority());
        }
        else if (position instanceof DeclarativeInlayPosition.EndOfLinePosition) {
            Editor editor = inlay.getEditor();
            return new DeclarativeInlayPosition.EndOfLinePosition(editor.getDocument().getLineNumber(newOffset));
        }
        else if (position instanceof DeclarativeInlayPosition.InlineInlayPosition) {
            DeclarativeInlayPosition.InlineInlayPosition orig = (DeclarativeInlayPosition.InlineInlayPosition) position;
            return new DeclarativeInlayPosition.InlineInlayPosition(newOffset, orig.isRelatedToPrevious(), orig.getPriority());
        }
        throw new IllegalArgumentException("Unknown InlayPosition type: " + position.getClass());
    }

    private static InlayData copyAndUpdatePosition(InlayData data, Inlay<?> inlay) {
        DeclarativeInlayPosition updated = copyAndUpdateOffset(data.getPosition(), inlay);
        return new InlayData(
            updated,
            data.getTooltip(),
            data.getHintFormat(),
            data.getTree(),
            data.getProviderId(),
            data.isDisabled(),
            data.getPayloads(),
            data.getProviderClass(),
            data.getSourceId()
        );
    }
}
