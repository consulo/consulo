// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;

import java.awt.*;
import java.awt.event.MouseEvent;

public class OnHoverPresentation extends StaticDelegatePresentation {
    private final InlayPresentationFactory.HoverListener onHoverListener;

    public OnHoverPresentation(InlayPresentation presentation,
                               InlayPresentationFactory.HoverListener onHoverListener) {
        super(presentation);
        this.onHoverListener = onHoverListener;
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        super.mouseMoved(event, translated);
        onHoverListener.onHover(event, translated);
    }

    @Override
    public void mouseExited() {
        super.mouseExited();
        onHoverListener.onHoverFinished();
    }
}
