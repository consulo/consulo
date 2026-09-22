// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ui.annotation.RequiredUIAccess;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * This class has some unavoidable problems: update happens on every pass, update is recursive and may be even unnecessary.
 * New classes must not use this implementation!
 */
public class RecursivelyUpdatingRootPresentation extends BasePresentation implements RootInlayPresentation<InlayPresentation> {
    private InlayPresentation current;
    private PresentationListener listener;

    public RecursivelyUpdatingRootPresentation(InlayPresentation current) {
        this.current = current;
        this.listener = new MyPresentationListener();
        this.current.addListener(listener);
    }

    @Override
    @RequiredUIAccess
    public boolean update(InlayPresentation newPresentationContent, Editor editor, InlayPresentationFactory factory) {
        InlayPresentation previous = this.current;
        previous.removeListener(listener);
        this.current = newPresentationContent;
        this.listener = new MyPresentationListener();
        this.current.addListener(listener);

        Dimension previousDimension = new Dimension(previous.getWidth(), previous.getHeight());
        boolean updated = newPresentationContent.updateState(previous);
        if (updated) {
            fireContentChanged(new Rectangle(0, 0, getWidth(), getHeight()));
            Dimension currentDimension = new Dimension(getWidth(), getHeight());
            if (!previousDimension.equals(currentDimension)) {
                fireSizeChanged(previousDimension, currentDimension);
            }
        }
        return updated;
    }

    @Override
    public InlayPresentation getContent() {
        return current;
    }

    @Override
    public ContentKey<InlayPresentation> getKey() {
        return KEY;
    }

    @Override
    @RequiredUIAccess
    public int getWidth() {
        return current.getWidth();
    }

    @Override
    @RequiredUIAccess
    public int getHeight() {
        return current.getHeight();
    }

    @Override
    @RequiredUIAccess
    public void paint(Graphics2D g, TextAttributes attributes) {
        current.paint(g, attributes);
    }

    @Override
    public String toString() {
        return current.toString();
    }

    @Override
    @RequiredUIAccess
    public void mouseClicked(MouseEvent event, Point translated) {
        current.mouseClicked(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mousePressed(MouseEvent event, Point translated) {
        current.mousePressed(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(MouseEvent event, Point translated) {
        current.mouseMoved(event, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseExited() {
        current.mouseExited();
    }

    private static final ContentKey<InlayPresentation> KEY = new InlayKey<>("recursive.update.root");

    private class MyPresentationListener implements PresentationListener {
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
