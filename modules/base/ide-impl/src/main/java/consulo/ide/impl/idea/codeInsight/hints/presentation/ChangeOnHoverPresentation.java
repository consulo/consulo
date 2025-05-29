// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ChangeOnHoverPresentation extends StatefulPresentation<ChangeOnHoverPresentation.State> {
    private final InlayPresentation noHover;
    private final Supplier<InlayPresentation> hover;
    private final Predicate<MouseEvent> onHoverPredicate;

    public ChangeOnHoverPresentation(InlayPresentation noHover,
                                     Supplier<InlayPresentation> hover) {
        this(noHover, hover, mouseEvent -> true);
    }

    public ChangeOnHoverPresentation(InlayPresentation noHover,
                                     Supplier<InlayPresentation> hover,
                                     Predicate<MouseEvent> onHoverPredicate) {
        super(new State(false), STATE_MARK);
        this.noHover = noHover;
        this.hover = hover;
        this.onHoverPredicate = onHoverPredicate;
    }

    @Override
    public InlayPresentation getPresentation() {
        return getState().isInside() ? hover.get() : noHover;
    }

    @Override
    public String toString() {
        return (getState().isInside() ? "<hovered>" : "") + getPresentation().toString();
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        super.mouseMoved(event, translated);
        if (!onHoverPredicate.test(event)) {
            setState(new State(false));
            return;
        }
        if (!getState().isInside()) {
            setState(new State(true));
        }
    }

    @Override
    public void mouseExited() {
        if (getState().isInside()) {
            setState(new State(false));
        }
        super.mouseExited();
    }

    public static class State {
        private final boolean isInside;

        public State(boolean isInside) {
            this.isInside = isInside;
        }

        public boolean isInside() {
            return isInside;
        }
    }

    public static final StateMark<State> STATE_MARK = new StateMark<>("OnHover");
}
