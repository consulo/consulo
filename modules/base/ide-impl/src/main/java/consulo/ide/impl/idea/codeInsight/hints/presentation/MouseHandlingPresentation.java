// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;

import java.awt.*;
import java.awt.event.MouseEvent;

public class MouseHandlingPresentation extends StaticDelegatePresentation {
    private final InlayPresentationFactory.ClickListener clickListener;
    private final InlayPresentationFactory.HoverListener hoverListener;

    public MouseHandlingPresentation(InlayPresentation presentation,
                                     InlayPresentationFactory.ClickListener clickListener,
                                     InlayPresentationFactory.HoverListener hoverListener) {
        super(presentation);
        this.clickListener = clickListener;
        this.hoverListener = hoverListener;
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        super.mouseClicked(event, translated);
        if (clickListener != null) {
            clickListener.onClick(event, translated);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        super.mouseMoved(event, translated);
        if (hoverListener != null) {
            hoverListener.onHover(event, translated);
        }
    }

    @Override
    public void mouseExited() {
        super.mouseExited();
        if (hoverListener != null) {
            hoverListener.onHoverFinished();
        }
    }
}
