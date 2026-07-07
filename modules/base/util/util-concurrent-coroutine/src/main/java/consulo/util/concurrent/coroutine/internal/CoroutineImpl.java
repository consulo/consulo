//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine.internal;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The default implementation of {@link Coroutine}.
 *
 * @author eso
 */
public class CoroutineImpl<I extends @Nullable Object, O extends @Nullable Object> extends UserDataHolderBase implements Coroutine<I, O> {
    private @Nullable StepChain<I, ?, O> code;

    private @Nullable String name;

    /**
     * Creates a new instance that starts execution with a certain step.
     *
     * @param firstStep The first step to execute
     */
    public CoroutineImpl(CoroutineStep<I, O> firstStep) {
        Objects.requireNonNull(firstStep);

        init(new StepChain<>(firstStep, new FinishStep<>()), null);
    }

    /**
     * Creates a new uninitialized instance.
     */
    CoroutineImpl() {
    }

    /**
     * Copies a coroutine for execution.
     *
     * @param other The coroutine to copy the definition from
     */
    private CoroutineImpl(CoroutineImpl<I, O> other) {
        init(other.getRequiredCode(), other);
    }

    /**
     * Internal constructor to create a new instance that is an extension of
     * another coroutine.
     *
     * @param other    The other coroutine
     * @param nextStep The code to execute after that of the other coroutine
     */
    private <T> CoroutineImpl(CoroutineImpl<I, T> other, CoroutineStep<T, O> nextStep) {
        Objects.requireNonNull(nextStep);

        init(other.getRequiredCode().then(nextStep), other);
    }

    @Override
    public Continuation<O> runAsync(CoroutineScope scope, I input) {
        CoroutineImpl<I, O> aRunCoroutine = new CoroutineImpl<>(this);
        Continuation<O> aContinuation = new ContinuationImpl<>(scope, aRunCoroutine);

        CompletableFuture<I> fExecution = CompletableFuture.supplyAsync(() -> input, aContinuation);

        aRunCoroutine.getRequiredCode().runAsync(fExecution, null, aContinuation);

        return aContinuation;
    }

    @Override
    public Continuation<O> runBlocking(CoroutineScope scope, I input) {
        CoroutineImpl<I, O> aRunCoroutine = new CoroutineImpl<>(this);
        Continuation<O> aContinuation = new ContinuationImpl<>(scope, aRunCoroutine);

        aRunCoroutine.getRequiredCode().runBlocking(input, aContinuation);

        return aContinuation;
    }

    @Override
    public <T> Coroutine<I, T> then(CoroutineStep<O, T> step) {
        return new CoroutineImpl<>(this, step);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", name, code);
    }

    @Override
    public @Nullable String getName() {
        return name;
    }

    @Override
    public Coroutine<I, O> withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the {@link StepChain} object containing the code of this
     * coroutine.
     */
    @Nullable StepChain<I, ?, O> getCode() {
        return code;
    }

    /**
     * Returns the {@link StepChain} object containing the code of this
     * coroutine.
     */
    StepChain<I, ?, O> getRequiredCode() {
        return Objects.requireNonNull(code);
    }

    /**
     * Initializes a new instance. Invoked from constructors.
     *
     * @param code  The code to be executed
     * @param other Another coroutine to copy configuration data from or NULL
     *              for none
     */
    void init(StepChain<I, ?, O> code, @Nullable CoroutineImpl<?, ?> other) {
        this.code = code;

        name = getClass().getSimpleName().replaceAll("Impl$", "");

        if (other != null) {
            other.copyCopyableDataTo(this);
        }
    }

    /**
     * Terminates the asynchronous execution of this coroutine by invoking it's
     * last step with an input value of NULL. This method should be invoked by
     * steps that need to end the execution of their coroutine early (e.g. if a
     * condition is not met).
     *
     * @param continuation The continuation of the execution
     */
    public void terminate(Continuation<?> continuation) {
        Objects.requireNonNull(getRequiredCode().getLastStep())
            .runAsync(CompletableFuture.supplyAsync(() -> null, continuation), null, continuation);
    }

    /**
     * The final step of a coroutine execution that updates the state of the
     * corresponding {@link Continuation}.
     *
     * @author eso
     */
    static class FinishStep<T> extends CoroutineStep<T, T> {

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        protected @Nullable T execute(@Nullable T input, Continuation<?> continuation) {
            // as this is the finish step, it must have the same type T as the
            // continuation result
            ((ContinuationImpl<T>) continuation).finish(input);

            return input;
        }
    }

    /**
     * A chaining of two coroutine steps. Next steps can also be step chains, so
     * that arbitrary complex call sequences can be created.
     *
     * @author eso
     */
    static class StepChain<I, T, O> extends CoroutineStep<I, O> {

        CoroutineStep<I, T> step;

        @Nullable CoroutineStep<T, O> next;

        /**
         * Creates a new instance.
         *
         * @param step The first execution
         * @param next The second execution
         */
        private StepChain(CoroutineStep<I, T> step, @Nullable CoroutineStep<T, O> next) {
            this.step = step;
            this.next = next;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void runAsync(
            CompletableFuture<I> previousExecution,
            @Nullable CoroutineStep<O, ?> nextStep,
            Continuation<?> continuation
        ) {
            if (!continuation.isCancelled()) {
                try {
                    ((ContinuationImpl<?>) continuation).trace(step);

                    // A step chain will always be a second step and is thus
                    // invoked with a next step argument of NULL. Therefore, the
                    // next step of the chain is used here.
                    step.runAsync(previousExecution, next, continuation);
                }
                catch (Throwable e) {
                    continuation.fail(e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return step + " -> " + next;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("NullAway")
        protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
            if (continuation.isCancelled()) {
                return null;
            }
            else {
                try {
                    ((ContinuationImpl<?>) continuation).trace(step);

                    return Objects.requireNonNull(next).runBlocking(step.runBlocking(input, continuation), continuation);
                }
                catch (Throwable e) {
                    continuation.fail(e);

                    return null;
                }
            }
        }

        /**
         * Returns the last step in this chain.
         *
         * @return The last step
         */
        @Nullable CoroutineStep<?, ?> getLastStep() {
            if (next instanceof StepChain) {
                return ((StepChain<?, ?, ?>) next).getLastStep();
            }
            else {
                return next;
            }
        }

        /**
         * Returns an extended {@link StepChain} that invokes a certain step at
         * the end.
         *
         * @param step The next step to invoke
         * @return The new invocation
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        <R> StepChain<I, T, R> then(CoroutineStep<O, R> step) {
            StepChain<I, T, R> aChainedInvocation = new StepChain<>(this.step, null);

            if (next instanceof StepChain) {
                // Chains need to be accessed as raw types because the
                // intermediate type of the chain in rNextStep is unknown
                aChainedInvocation.next = ((StepChain) next).then(step);
            }
            else {
                // rNextStep is either another StepChain (see above) or else the
                // FinishStep which must be invoked last. Raw type is necessary
                // because the type of THIS is actually <I,O,O> as FinishStep is
                // an identity step, but this type info is not available here.
                aChainedInvocation.next = new StepChain(step, next);
            }
            return aChainedInvocation;
        }

        /**
         * Returns a copy of this {@link StepChain} with the last (finish) step
         * replaced with the argument step.
         *
         * @param step The last step to invoke
         * @return The new invocation
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        <R> StepChain<I, T, R> withLastStep(CoroutineStep<?, R> step) {
            StepChain<I, T, R> aChainedInvocation = new StepChain<>(this.step, null);

            if (next instanceof StepChain) {
                // Chains need to be accessed as raw types because the
                // intermediate type of the chain in rNextStep is unknown
                aChainedInvocation.next = ((StepChain) next).withLastStep(step);
            }
            else {
                // step needs to be cast because the actual types at the end of
                // the chain are not known here
                aChainedInvocation.next = (CoroutineStep<T, R>) step;
            }
            return aChainedInvocation;
        }
    }
}
