// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.trace.BidirectionalAwareState;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.wrapper.StreamCall;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
final class IntermediateStateImpl extends StateBase implements BidirectionalAwareState {
    private final Map<TraceElement, List<TraceElement>> myToPrev;
    private final Map<TraceElement, List<TraceElement>> myToNext;
    private final StreamCall myNextCall;
    private final StreamCall myPrevCall;

    IntermediateStateImpl(List<TraceElement> elements,
                          StreamCall prevCall, StreamCall nextCall,
                          Map<TraceElement, List<TraceElement>> toPrevMapping,
                          Map<TraceElement, List<TraceElement>> toNextMapping) {
        super(elements);
        myToPrev = toPrevMapping;
        myToNext = toNextMapping;

        myPrevCall = prevCall;
        myNextCall = nextCall;
    }

    @Override
    public StreamCall getPrevCall() {
        return myPrevCall;
    }

    @Override
    public List<TraceElement> getPrevValues(TraceElement value) {
        return myToPrev.getOrDefault(value, Collections.emptyList());
    }

    @Override
    public StreamCall getNextCall() {
        return myNextCall;
    }

    @Override
    public List<TraceElement> getNextValues(TraceElement value) {
        return myToNext.getOrDefault(value, Collections.emptyList());
    }
}
