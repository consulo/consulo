// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import org.jetbrains.annotations.TestOnly;

import java.util.Collections;
import java.util.List;

public class DeclarativeInlayRenderer extends DeclarativeInlayRendererBase<InlayData> {
    private final CompositeDeclarativeHintWithMarginsView.SingleDeclarativeHintView view;

    public DeclarativeInlayRenderer(InlayData inlayData,
                                    InlayTextMetricsStorage fontMetricsStorage,
                                    String providerId,
                                    String sourceId) {
        super(providerId, sourceId, fontMetricsStorage);
        this.view = new CompositeDeclarativeHintWithMarginsView.SingleDeclarativeHintView(inlayData);
    }

    @Override
    protected DeclarativeHintView<InlayData> getView() {
        return view;
    }

    @Override
    public List<InlayPresentationList> getPresentationLists() {
        return Collections.singletonList(getPresentationList());
    }

    @TestOnly
    public InlayPresentationList getPresentationList() {
        return view.getPresentationList();
    }
}
