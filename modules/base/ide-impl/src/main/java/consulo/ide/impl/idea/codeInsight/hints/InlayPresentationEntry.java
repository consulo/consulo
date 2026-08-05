// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.codeEditor.InlayContentSegment;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

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

    public void setHoveredWithCtrl(boolean hoveredWithCtrl) {
        isHoveredWithCtrl = hoveredWithCtrl;
    }

    @TestOnly
    public InlayMouseArea getClickArea() {
        return clickArea;
    }

    /**
     * Whether a click on this entry reaches an action handler - only the entries a provider gave an action to do.
     */
    public boolean hasClickAction() {
        return clickArea != null && clickArea.getActionData() != null;
    }

    public abstract void render(Graphics2D graphics,
                                InlayTextMetrics metrics,
                                TextAttributes attributes,
                                boolean isDisabled,
                                int yOffset,
                                int rectHeight,
                                Editor editor);

    /**
     * The entry as a run of an {@link consulo.codeEditor.InlayContent}, for the frontends which cannot call
     * {@link #render}.
     *
     * @param attributesKey the colour the whole presentation resolved to, since the kind is held by the list rather
     *                      than by its entries
     */
    public abstract InlayContentSegment toContentSegment(@Nullable TextAttributesKey attributesKey);

    public abstract int computeWidth(InlayTextMetrics metrics);

    public abstract int computeHeight(InlayTextMetrics metrics);

    public abstract void handleClick(EditorMouseEvent e,
                                     InlayPresentationList list,
                                     boolean controlDown);
}
