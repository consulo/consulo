// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.BasePresentation;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;
import consulo.ui.annotation.RequiredUIAccess;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Stateless presentation, that delegates to another {@link InlayPresentation}, which can be dynamically changed.
 */
public class DynamicDelegatePresentation extends BasePresentation {
    private InlayPresentation delegate;
    private PresentationListener listener;

    public DynamicDelegatePresentation(InlayPresentation delegate) {
        this.delegate = delegate;
        this.listener = new DelegateListener();
        delegate.addListener(this.listener);
    }

    public InlayPresentation getDelegate() {
        return delegate;
    }

    @RequiredUIAccess
    public void setDelegate(InlayPresentation newDelegate) {
        Dimension previousDim = getDimension(delegate);
        Dimension newDim = getDimension(newDelegate);
        delegate.removeListener(listener);
        this.delegate = newDelegate;
        this.listener = new DelegateListener();
        newDelegate.addListener(listener);
        if (!previousDim.equals(newDim)) {
            fireSizeChanged(previousDim, newDim);
        }
        fireContentChanged(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    @RequiredUIAccess
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    @RequiredUIAccess
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    @RequiredUIAccess
    public boolean updateState(InlayPresentation previousPresentation) {
        if (!(previousPresentation instanceof DynamicDelegatePresentation dynamicDelegatePresentation)) {
            fireSizeChanged(getDimension(delegate), getDimension(delegate));
            return true;
        }
        return delegate.updateState(dynamicDelegatePresentation.delegate);
    }

    @Override
    @RequiredUIAccess
    public void paint(Graphics2D g, TextAttributes attributes) {
        delegate.paint(g, attributes);
    }

    @Override
    @RequiredUIAccess
    public void mouseClicked(MouseEvent event, Point translated) {
        delegate.mouseClicked(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(MouseEvent event, Point translated) {
        delegate.mouseMoved(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseExited() {
        delegate.mouseExited();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @RequiredUIAccess
    private Dimension getDimension(InlayPresentation presentation) {
        return new Dimension(presentation.getWidth(), presentation.getHeight());
    }

    private class DelegateListener implements PresentationListener {
        @Override
        @RequiredUIAccess
        public void contentChanged(Rectangle area) {
            fireContentChanged(area);
        }

        @Override
        @RequiredUIAccess
        public void sizeChanged(Dimension previous, Dimension current) {
            fireSizeChanged(previous, current);
        }
    }
}
