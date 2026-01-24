// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.lib.ResolverFactory;
import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.resolve.ResolvedStreamChain;
import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.resolve.impl.ResolvedIntermediateCallImpl;
import consulo.execution.debug.stream.resolve.impl.ResolvedStreamChainImpl;
import consulo.execution.debug.stream.resolve.impl.ResolvedTerminatorCallImpl;
import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.execution.debug.stream.wrapper.TraceUtil;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public class TracingResultImpl implements TracingResult {
    private final TraceElement myStreamResult;
    private final List<TraceInfo> myTrace;
    private final boolean myIsResultException;
    private final StreamChain mySourceChain;

    TracingResultImpl(@Nonnull StreamChain chain,
                      @Nonnull TraceElement streamResult,
                      @Nonnull List<TraceInfo> trace,
                      boolean isResultException) {
        myStreamResult = streamResult;
        myTrace = trace;
        mySourceChain = chain;
        myIsResultException = isResultException;
    }

    @Override
    public @Nonnull TraceElement getResult() {
        return myStreamResult;
    }

    @Override
    public boolean exceptionThrown() {
        return myIsResultException;
    }

    @Override
    public @Nonnull List<TraceInfo> getTrace() {
        return myTrace;
    }

    @Override
    public @Nonnull ResolvedTracingResult resolve(@Nonnull ResolverFactory resolverFactory) {
        assert myTrace.size() == mySourceChain.length();

        List<ValuesOrderResolver.Result> resolvedTraces =
            ContainerUtil.map(myTrace, x -> resolverFactory.getResolver(x.getCall().getName(), x.getCall().getType()).resolve(x));

        TraceInfo firstCallTrace = myTrace.get(0);
        List<IntermediateStreamCall> intermediateCalls = mySourceChain.getIntermediateCalls();

        ResolvedStreamChainImpl.Builder chainBuilder = new ResolvedStreamChainImpl.Builder();
        List<TraceElement> valuesBeforeFirstCall = TraceUtil.sortedByTime(firstCallTrace.getValuesOrderBefore().values());
        FirstStateImpl firstState = new FirstStateImpl(valuesBeforeFirstCall, firstCallTrace.getCall(),
            resolvedTraces.get(0).getDirectOrder());

        if (intermediateCalls.isEmpty()) {
            chainBuilder.setTerminator(buildResolvedTerminationCall(myTrace.get(0), firstState, resolvedTraces.get(0).getReverseOrder()));
        }
        else {
            ArrayList<IntermediateStateImpl> states = new ArrayList<>();

            for (int i = 0; i < intermediateCalls.size() - 1; i++) {
                states.add(new IntermediateStateImpl(TraceUtil.sortedByTime(myTrace.get(i).getValuesOrderAfter().values()),
                    intermediateCalls.get(i),
                    intermediateCalls.get(i + 1),
                    resolvedTraces.get(i).getReverseOrder(),
                    resolvedTraces.get(i + 1).getDirectOrder()));
            }

            states.add(new IntermediateStateImpl(TraceUtil.sortedByTime(myTrace.get(myTrace.size() - 1).getValuesOrderBefore().values()),
                intermediateCalls.get(intermediateCalls.size() - 1),
                mySourceChain.getTerminationCall(),
                resolvedTraces.get(resolvedTraces.size() - 2).getReverseOrder(),
                resolvedTraces.get(resolvedTraces.size() - 1).getDirectOrder()));

            chainBuilder.addIntermediate(new ResolvedIntermediateCallImpl(intermediateCalls.get(0), firstState, states.get(0)));
            for (int i = 1; i < states.size(); i++) {
                chainBuilder.addIntermediate(new ResolvedIntermediateCallImpl(intermediateCalls.get(i), states.get(i - 1), states.get(i)));
            }

            chainBuilder.setTerminator(buildResolvedTerminationCall(myTrace.get(myTrace.size() - 1), states.get(states.size() - 1),
                resolvedTraces.get(resolvedTraces.size() - 1).getReverseOrder()));
        }

        return new MyResolvedResult(chainBuilder.build());
    }

    private ResolvedStreamCall.Terminator buildResolvedTerminationCall(@Nonnull TraceInfo terminatorTrace,
                                                                       @Nonnull NextAwareState previousState,
                                                                       @Nonnull Map<TraceElement, List<TraceElement>>
                                                                           terminationToPrevMapping) {
        List<TraceElement> after = TraceUtil.sortedByTime(terminatorTrace.getValuesOrderAfter().values());
        TerminationStateImpl terminatorState =
            new TerminationStateImpl(myStreamResult, previousState.getNextCall(), after, terminationToPrevMapping);
        return new ResolvedTerminatorCallImpl(mySourceChain.getTerminationCall(), previousState, terminatorState);
    }

    private class MyResolvedResult implements ResolvedTracingResult {

        private final @Nonnull ResolvedStreamChain myChain;

        MyResolvedResult(@Nonnull ResolvedStreamChain resolvedStreamChain) {
            myChain = resolvedStreamChain;
        }

        @Override
        public @Nonnull ResolvedStreamChain getResolvedChain() {
            return myChain;
        }

        @Override
        public @Nonnull StreamChain getSourceChain() {
            return mySourceChain;
        }

        @Override
        public boolean exceptionThrown() {
            return myIsResultException;
        }

        @Override
        public @Nonnull TraceElement getResult() {
            return myStreamResult;
        }
    }
}
