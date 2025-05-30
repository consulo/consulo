// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

/**
 * Presentation that changes once it is first clicked.
 */
public class ChangeOnClickPresentation extends StatefulPresentation<ChangeOnClickPresentation.State> {
    private final InlayPresentation notClicked;
    private final Supplier<InlayPresentation> onClick;
    private InlayPresentation cached;

    public ChangeOnClickPresentation(InlayPresentation notClicked, Supplier<InlayPresentation> onClick) {
        super(new State(false), ourMark);
        this.notClicked = notClicked;
        this.onClick = onClick;
    }

    private InlayPresentation getClickedPresentation() {
        if (cached == null) {
            cached = onClick.get();
        }
        return cached;
    }

    @Override
    public InlayPresentation getPresentation() {
        return getState().clicked ? getClickedPresentation() : notClicked;
    }

    @Override
    public String toString() {
        return (getState().clicked ? "<clicked>" : "") + getCurrentPresentation().toString();
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        if (getState().clicked) {
            super.mouseClicked(event, translated);
        }
        else {
            setState(new State(true));
        }
    }

    public static class State {
        public final boolean clicked;

        public State(boolean clicked) {
            this.clicked = clicked;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof State)) return false;
            State other = (State) obj;
            return this.clicked == other.clicked;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(clicked);
        }

        @Override
        public String toString() {
            return "State(clicked=" + clicked + ")";
        }
    }

    public static final StateMark<State> ourMark = new StateMark<>("ChangeOnClick");
}
