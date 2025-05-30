// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.BasePresentation;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * Presentation with an internal state that affects rendering.
 * The state must be immutable and implement proper equals.
 */
public abstract class StatefulPresentation<S> extends BasePresentation {
    private S state;
    protected final StateMark<S> stateMark;
    private InlayPresentation currentPresentation;
    private PresentationListener listener = new DelegateListener();

    protected StatefulPresentation(S initialState, StateMark<S> stateMark) {
        this.state = initialState;
        this.stateMark = stateMark;
    }

    public S getState() {
        return state;
    }

    public void setState(S newState) {
        if (!Objects.equals(state, newState)) {
            Dimension previous = getDimension();
            updateStateAndPresentation(newState);
            fireSizeChanged(previous, getDimension());
        }
    }

    private void updateStateAndPresentation(S newState) {
        Dimension previous = getDimension();
        this.state = newState;
        InlayPresentation newPresentation = getPresentation();
        updatePresentation(newPresentation);
        fireSizeChanged(previous, getDimension());
    }

    private void updatePresentation(InlayPresentation presentation) {
        if (currentPresentation != null) {
            currentPresentation.removeListener(listener);
        }
        presentation.addListener(listener);
        this.currentPresentation = presentation;
    }

    public InlayPresentation getCurrentPresentation() {
        if (currentPresentation == null) {
            updatePresentation(getPresentation());
        }
        return currentPresentation;
    }

    @Override
    public int getWidth() {
        return getCurrentPresentation().getWidth();
    }

    @Override
    public int getHeight() {
        return getCurrentPresentation().getHeight();
    }

    @Override
    public boolean updateState(InlayPresentation previousPresentation) {
        if (!(previousPresentation instanceof StatefulPresentation)) return true;

        @SuppressWarnings("unchecked")
        StatefulPresentation<S> prev = (StatefulPresentation<S>) previousPresentation;
        S prevState = prev.state;
        boolean stateChanged = false;

        if (!Objects.equals(prevState, this.state) && this.stateMark.equals(prev.stateMark)) {
            S casted = stateMark.cast(prevState, prev.stateMark);
            if (casted != null) {
                updateStateAndPresentation(casted);
                stateChanged = true;
            }
        }

        boolean presentationChanged = getCurrentPresentation().updateState(prev.getCurrentPresentation());
        return stateChanged || presentationChanged;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        getCurrentPresentation().paint(g, attributes);
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        getCurrentPresentation().mouseClicked(event, translated);
    }

    @Override
    public void mousePressed(MouseEvent event, Point translated) {
        getCurrentPresentation().mousePressed(event, translated);
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        getCurrentPresentation().mouseMoved(event, translated);
    }

    @Override
    public void mouseExited() {
        getCurrentPresentation().mouseExited();
    }

    private Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    private class DelegateListener implements PresentationListener {
        @Override
        public void contentChanged(Rectangle area) {
            fireContentChanged(area);
        }

        @Override
        public void sizeChanged(Dimension previous, Dimension current) {
            fireSizeChanged(previous, current);
        }
    }

    /**
     * Used to mark and validate state identity for type safety.
     */
    public static class StateMark<T> {
        private final String id;

        public StateMark(String id) {
            this.id = id;
        }

        public T cast(Object value, StateMark<?> otherMark) {
            if (!this.equals(otherMark)) return null;
            @SuppressWarnings("unchecked")
            T casted = (T) value;
            return casted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateMark)) return false;
            StateMark<?> that = (StateMark<?>) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "StateMark(" + id + ")";
        }
    }

    /**
     * Must return actual presentation based on the current state.
     */
    protected abstract InlayPresentation getPresentation();
}
