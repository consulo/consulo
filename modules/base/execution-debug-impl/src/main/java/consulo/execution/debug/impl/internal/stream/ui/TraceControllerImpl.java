// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.ui.PropagationDirection;
import consulo.execution.debug.stream.ui.TraceContainer;
import consulo.execution.debug.stream.ui.TraceController;
import consulo.execution.debug.stream.ui.ValuesSelectionListener;
import consulo.execution.debug.stream.wrapper.StreamCall;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class TraceControllerImpl implements TraceController, Disposable {
    private final List<TraceContainer> myTraceContainers = new CopyOnWriteArrayList<>();
    private final ValuesSelectionListener mySelectionListener;
    private final IntermediateState myState;
    private final PrevAwareState myToPrev;
    private final NextAwareState myToNext;

    private TraceController myPrevListener = null;
    private TraceController myNextListener = null;

    TraceControllerImpl(IntermediateState state) {
        myState = state;
        myToPrev = state instanceof PrevAwareState ? (PrevAwareState) state : null;
        myToNext = state instanceof NextAwareState ? (NextAwareState) state : null;

        mySelectionListener = elements -> {
            selectAll(elements);

            propagateForward(elements);
            propagateBackward(elements);
        };
    }

    @Override
    public void dispose() {
    }

    void setPreviousController(TraceController listener) {
        myPrevListener = listener;
    }

    void setNextController(TraceController listener) {
        myNextListener = listener;
    }

    @Override
    public @Nullable Value getStreamResult() {
        return myState.getStreamResult();
    }

    @Override
    public List<TraceElement> getTrace() {
        return myState.getTrace();
    }

    @Override
    public @Nullable StreamCall getNextCall() {
        return myToNext == null ? null : myToNext.getNextCall();
    }

    @Override
    public @Nullable StreamCall getPrevCall() {
        return myToPrev == null ? null : myToPrev.getPrevCall();
    }

    @Override
    public List<TraceElement> getNextValues(TraceElement element) {
        return myToNext == null ? Collections.emptyList() : myToNext.getNextValues(element);
    }

    @Override
    public List<TraceElement> getPrevValues(TraceElement element) {
        return myToPrev == null ? Collections.emptyList() : myToPrev.getPrevValues(element);
    }

    @Override
    public boolean isSelectionExists(PropagationDirection direction) {
        for (TraceContainer container : myTraceContainers) {
            if (container.highlightedExists()) {
                return true;
            }
        }

        return PropagationDirection.FORWARD.equals(direction)
            ? selectionExistsForward()
            : selectionExistsBackward();
    }

    @Override
    public void register(TraceContainer listener) {
        myTraceContainers.add(listener);
        listener.addSelectionListener(mySelectionListener);
        Disposer.register(this, listener);
    }

    @Override
    public void highlightingChanged(List<TraceElement> values, PropagationDirection direction) {
        highlightAll(values);
        propagate(values, direction);
    }

    private void propagate(List<TraceElement> values, PropagationDirection direction) {
        if (direction.equals(PropagationDirection.BACKWARD)) {
            propagateBackward(values);
        }
        else {
            propagateForward(values);
        }
    }

    private void propagateForward(List<TraceElement> values) {
        if (myNextListener == null) {
            return;
        }
        final List<TraceElement> nextValues =
            values.stream().flatMap(x -> getNextValues(x).stream()).collect(Collectors.toList());
        myNextListener.highlightingChanged(nextValues, PropagationDirection.FORWARD);
    }

    private void propagateBackward(List<TraceElement> values) {
        if (myPrevListener == null) {
            return;
        }
        final List<TraceElement> prevValues =
            values.stream().flatMap(x -> getPrevValues(x).stream()).collect(Collectors.toList());
        myPrevListener.highlightingChanged(prevValues, PropagationDirection.BACKWARD);
    }

    private void highlightAll(List<TraceElement> values) {
        for (final TraceContainer listener : myTraceContainers) {
            listener.highlight(values);
        }
    }

    private void selectAll(List<TraceElement> values) {
        for (final TraceContainer listener : myTraceContainers) {
            listener.select(values);
        }
    }

    private boolean selectionExistsForward() {
        return myNextListener != null && myNextListener.isSelectionExists(PropagationDirection.FORWARD);
    }

    private boolean selectionExistsBackward() {
        return myPrevListener != null && myPrevListener.isSelectionExists(PropagationDirection.BACKWARD);
    }
}
