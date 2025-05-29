// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;

import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class StaticDelegatePresentation implements InlayPresentation {
    protected final InlayPresentation presentation;

    public StaticDelegatePresentation(InlayPresentation presentation) {
        this.presentation = presentation;
    }

    @Override
    public int getWidth() {
        return presentation.getWidth();
    }

    @Override
    public int getHeight() {
        return presentation.getHeight();
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        presentation.paint(g, attributes);
    }

    @Override
    public void fireSizeChanged(Dimension previous, Dimension current) {
        presentation.fireSizeChanged(previous, current);
    }

    @Override
    public void fireContentChanged(Rectangle area) {
        presentation.fireContentChanged(area);
    }

    @Override
    public void addListener(PresentationListener listener) {
        presentation.addListener(listener);
    }

    @Override
    public void removeListener(PresentationListener listener) {
        presentation.removeListener(listener);
    }

    @Override
    public boolean updateState(InlayPresentation previousPresentation) {
        if (!(previousPresentation instanceof StaticDelegatePresentation)) {
            return true;
        }
        return presentation.updateState(((StaticDelegatePresentation) previousPresentation).presentation);
    }

    @Override
    public String toString() {
        return presentation.toString();
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        presentation.mouseClicked(event, translated);
    }

    @Override
    public void mousePressed(MouseEvent event, Point translated) {
        presentation.mousePressed(event, translated);
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        presentation.mouseMoved(event, translated);
    }

    @Override
    public void mouseExited() {
        presentation.mouseExited();
    }
}
