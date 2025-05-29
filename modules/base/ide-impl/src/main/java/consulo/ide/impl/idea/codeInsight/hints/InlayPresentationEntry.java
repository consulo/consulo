// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;

public abstract class InlayPresentationEntry {
    @TestOnly
    protected final InlayMouseArea clickArea;
    public boolean isHoveredWithCtrl = false;

    protected InlayPresentationEntry(InlayMouseArea clickArea) {
        this.clickArea = clickArea;
    }

    public boolean isHoveredWithCtrl() {
        return isHoveredWithCtrl;
    }

    @TestOnly
    public InlayMouseArea getClickArea() {
        return clickArea;
    }

    public abstract void render(Graphics2D graphics,
                                InlayTextMetrics metrics,
                                TextAttributes attributes,
                                boolean isDisabled,
                                int yOffset,
                                int rectHeight,
                                Editor editor);

    public abstract int computeWidth(InlayTextMetrics metrics);

    public abstract int computeHeight(InlayTextMetrics metrics);

    public abstract void handleClick(EditorMouseEvent e,
                                     InlayPresentationList list,
                                     boolean controlDown);
}
