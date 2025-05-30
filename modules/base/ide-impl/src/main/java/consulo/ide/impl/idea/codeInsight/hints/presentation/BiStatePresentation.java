// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;

import java.util.function.Supplier;

/**
 * Presentation that may be in two states and can preserve state type between passes.
 */
public class BiStatePresentation extends StatefulPresentation<BiStatePresentation.State> {
    private final Supplier<InlayPresentation> first;
    private final Supplier<InlayPresentation> second;

    public BiStatePresentation(Supplier<InlayPresentation> first,
                               Supplier<InlayPresentation> second,
                               boolean initiallyFirstEnabled) {
        super(new State(initiallyFirstEnabled), STATE_MARK);
        this.first = first;
        this.second = second;
    }

    @Override
    protected InlayPresentation getPresentation() {
        return getState().currentFirst ? first.get() : second.get();
    }

    public void flipState() {
        setState(new State(!getState().currentFirst));
    }

    public void setFirst() {
        setState(new State(true));
    }

    public void setSecond() {
        setState(new State(false));
    }

    @Override
    public String toString() {
        return getCurrentPresentation().toString();
    }

    public static class State {
        public final boolean currentFirst;

        public State(boolean currentFirst) {
            this.currentFirst = currentFirst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof State)) {
                return false;
            }
            State state = (State) o;
            return currentFirst == state.currentFirst;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(currentFirst);
        }

        @Override
        public String toString() {
            return "State(currentFirst=" + currentFirst + ")";
        }
    }

    public static final StateMark<State> STATE_MARK = new StateMark<>("BiState");
}
