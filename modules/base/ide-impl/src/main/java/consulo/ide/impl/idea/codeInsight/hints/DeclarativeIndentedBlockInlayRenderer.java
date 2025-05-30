// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Inlay;

import java.util.List;

public class DeclarativeIndentedBlockInlayRenderer extends DeclarativeInlayRendererBase<List<InlayData>> {
    private final IndentedDeclarativeHintView<CompositeDeclarativeHintWithMarginsView.MultipleDeclarativeHintsView, List<InlayData>> view;

    public DeclarativeIndentedBlockInlayRenderer(List<InlayData> inlayData,
                                                 InlayTextMetricsStorage fontMetricsStorage,
                                                 String providerId,
                                                 String sourceId,
                                                 int initialIndentAnchorOffset) {
        super(providerId, sourceId, fontMetricsStorage);
        this.view = new IndentedDeclarativeHintView<>(
            new CompositeDeclarativeHintWithMarginsView.MultipleDeclarativeHintsView(inlayData),
            initialIndentAnchorOffset
        );
    }

    @Override
    protected DeclarativeHintView<List<InlayData>> getView() {
        return view;
    }

    @Override
    public List<InlayPresentationList> getPresentationLists() {
        return view.getView().getPresentationLists();
    }

    @Override
    public void initInlay(Inlay<? extends DeclarativeInlayRendererBase<List<InlayData>>> inlay) {
        super.initInlay(inlay);
        view.setInlay(inlay);
    }
}
