// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

public interface DeclarativeHintViewWithMargins extends DeclarativeHintView<InlayData> {
    /**
     * The horizontal margin before the hint content.
     */
    int getMargin();

    /**
     * Calculates the total width of the hint box, including margins and padding.
     *
     * @param storage     storage for text metrics
     * @param forceUpdate whether to force recomputation of cached widths
     */
    int getBoxWidth(InlayTextMetricsStorage storage, boolean forceUpdate);
}