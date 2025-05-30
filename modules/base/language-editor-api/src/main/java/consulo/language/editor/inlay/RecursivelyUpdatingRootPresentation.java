// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;

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
    public boolean update(InlayPresentation newPresentationContent,
                          Editor editor,
                          InlayPresentationFactory factory) {
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
    public int getWidth() {
        return current.getWidth();
    }

    @Override
    public int getHeight() {
        return current.getHeight();
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        current.paint(g, attributes);
    }

    @Override
    public String toString() {
        return current.toString();
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        current.mouseClicked(event, translated);
    }

    @Override
    public void mousePressed(MouseEvent event, Point translated) {
        current.mousePressed(event, translated);
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        current.mouseMoved(event, translated);
    }

    @Override
    public void mouseExited() {
        current.mouseExited();
    }

    private static final ContentKey<InlayPresentation> KEY = new InlayKey<>("recursive.update.root");

    private class MyPresentationListener implements PresentationListener {
        @Override
        public void contentChanged(Rectangle area) {
            fireContentChanged(area);
        }

        @Override
        public void sizeChanged(Dimension previous, Dimension current) {
            fireSizeChanged(previous, current);
        }
    }
}
