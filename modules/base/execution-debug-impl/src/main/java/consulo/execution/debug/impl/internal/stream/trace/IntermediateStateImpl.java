// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.trace.BidirectionalAwareState;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;

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

    IntermediateStateImpl(@Nonnull List<TraceElement> elements,
                          @Nonnull StreamCall prevCall, @Nonnull StreamCall nextCall,
                          @Nonnull Map<TraceElement, List<TraceElement>> toPrevMapping,
                          @Nonnull Map<TraceElement, List<TraceElement>> toNextMapping) {
        super(elements);
        myToPrev = toPrevMapping;
        myToNext = toNextMapping;

        myPrevCall = prevCall;
        myNextCall = nextCall;
    }

    @Override
    public @Nonnull StreamCall getPrevCall() {
        return myPrevCall;
    }

    @Override
    public @Nonnull List<TraceElement> getPrevValues(@Nonnull TraceElement value) {
        return myToPrev.getOrDefault(value, Collections.emptyList());
    }

    @Override
    public @Nonnull StreamCall getNextCall() {
        return myNextCall;
    }

    @Override
    public @Nonnull List<TraceElement> getNextValues(@Nonnull TraceElement value) {
        return myToNext.getOrDefault(value, Collections.emptyList());
    }
}
