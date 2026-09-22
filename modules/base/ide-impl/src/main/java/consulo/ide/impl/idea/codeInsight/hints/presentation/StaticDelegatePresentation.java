// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;
import consulo.ui.annotation.RequiredUIAccess;

import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class StaticDelegatePresentation implements InlayPresentation {
    protected final InlayPresentation presentation;

    public StaticDelegatePresentation(InlayPresentation presentation) {
        this.presentation = presentation;
    }

    @Override
    @RequiredUIAccess
    public int getWidth() {
        return presentation.getWidth();
    }

    @Override
    @RequiredUIAccess
    public int getHeight() {
        return presentation.getHeight();
    }

    @Override
    @RequiredUIAccess
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
    @RequiredUIAccess
    public boolean updateState(InlayPresentation previousPresentation) {
        return !(previousPresentation instanceof StaticDelegatePresentation staticDelegatePresentation)
            || presentation.updateState(staticDelegatePresentation.presentation);
    }

    @Override
    public String toString() {
        return presentation.toString();
    }

    @Override
    @RequiredUIAccess
    public void mouseClicked(MouseEvent event, Point translated) {
        presentation.mouseClicked(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mousePressed(MouseEvent event, Point translated) {
        presentation.mousePressed(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(MouseEvent event, Point translated) {
        presentation.mouseMoved(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseExited() {
        presentation.mouseExited();
    }
}
