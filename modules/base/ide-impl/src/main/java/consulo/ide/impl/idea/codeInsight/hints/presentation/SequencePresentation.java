// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.BasePresentation;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SequencePresentation extends BasePresentation {
    private final List<InlayPresentation> presentations;
    private InlayPresentation presentationUnderCursor;

    public SequencePresentation(List<InlayPresentation> presentations) {
        if (presentations.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.presentations = presentations;
        for (InlayPresentation presentation : presentations) {
            presentation.addListener(new InternalListener(presentation));
        }
    }

    @Override
    public int getWidth() {
        int width = 0;
        for (InlayPresentation presentation : presentations) {
            width += presentation.getWidth();
        }
        return width;
    }

    @Override
    public int getHeight() {
        int height = 0;
        for (InlayPresentation presentation : presentations) {
            height = Math.max(height, presentation.getHeight());
        }
        return height;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        int xOffset = 0;
        try {
            for (InlayPresentation presentation : presentations) {
                presentation.paint(g, attributes);
                int w = presentation.getWidth();
                xOffset += w;
                g.translate(w, 0);
            }
        }
        finally {
            g.translate(-xOffset, 0);
        }
    }

    private void handleMouse(Point original, BiConsumer<InlayPresentation, Point> action) {
        int x = original.x;
        int y = original.y;
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return;
        }
        int xOffset = 0;
        for (InlayPresentation presentation : presentations) {
            int pw = presentation.getWidth();
            if (x < xOffset + pw) {
                if (y > presentation.getHeight()) {
                    changePresentationUnderCursor(null);
                    return;
                }
                changePresentationUnderCursor(presentation);
                Point translated = new Point(x - xOffset, y);
                action.accept(presentation, translated);
                return;
            }
            xOffset += pw;
        }
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        handleMouse(translated, (p, pt) -> p.mouseClicked(event, pt));
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        handleMouse(translated, (p, pt) -> p.mouseMoved(event, pt));
    }

    @Override
    public void mouseExited() {
        changePresentationUnderCursor(null);
    }

    @Override
    public boolean updateState(InlayPresentation previousPresentation) {
        if (!(previousPresentation instanceof SequencePresentation)) {
            return true;
        }
        SequencePresentation prev = (SequencePresentation) previousPresentation;
        if (prev.presentations.size() != presentations.size()) {
            return true;
        }
        boolean changed = false;
        List<InlayPresentation> prevList = prev.presentations;
        for (int i = 0; i < presentations.size(); i++) {
            if (presentations.get(i).updateState(prevList.get(i))) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public String toString() {
        return presentations.stream()
            .map(Object::toString)
            .collect(Collectors.joining(" ", "[", "]"));
    }

    private void changePresentationUnderCursor(InlayPresentation presentation) {
        if (presentationUnderCursor != presentation) {
            if (presentationUnderCursor != null) {
                presentationUnderCursor.mouseExited();
            }
            presentationUnderCursor = presentation;
        }
    }

    private class InternalListener implements PresentationListener {
        private final InlayPresentation currentPresentation;

        InternalListener(InlayPresentation currentPresentation) {
            this.currentPresentation = currentPresentation;
        }

        @Override
        public void contentChanged(Rectangle area) {
            area.add(shiftOfCurrent(), 0);
            SequencePresentation.this.fireContentChanged(area);
        }

        @Override
        public void sizeChanged(Dimension previous, Dimension current) {
            SequencePresentation.this.fireSizeChanged(previous, current);
        }

        private int shiftOfCurrent() {
            int shift = 0;
            for (InlayPresentation p : presentations) {
                if (p == currentPresentation) {
                    return shift;
                }
                shift += p.getWidth();
            }
            throw new IllegalStateException();
        }
    }
}
