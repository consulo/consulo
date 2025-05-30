// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public interface DeclarativeHintView<Model> {
    @RequiredUIAccess
    void updateModel(Model newModel);

    @RequiredUIAccess
    int calcWidthInPixels(Inlay<?> inlay, InlayTextMetricsStorage fontMetricsStorage);

    void paint(
        Inlay<?> inlay,
        Graphics2D g,
        Rectangle2D targetRegion,
        TextAttributes textAttributes,
        InlayTextMetricsStorage fontMetricsStorage
    );

    void handleLeftClick(
        EditorMouseEvent e,
        Point pointInsideInlay,
        InlayTextMetricsStorage fontMetricsStorage,
        boolean controlDown
    );

    LightweightHint handleHover(
        EditorMouseEvent e,
        Point pointInsideInlay,
        InlayTextMetricsStorage fontMetricsStorage
    );

    void handleRightClick(
        EditorMouseEvent e,
        Point pointInsideInlay,
        InlayTextMetricsStorage fontMetricsStorage
    );

    InlayMouseArea getMouseArea(
        Point pointInsideInlay,
        InlayTextMetricsStorage fontMetricsStorage
    );
}
