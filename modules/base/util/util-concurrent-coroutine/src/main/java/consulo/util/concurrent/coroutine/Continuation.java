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
package consulo.util.concurrent.coroutine;

import consulo.util.dataholder.CopyableUserDataHolder;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A continuation represents the state of a coroutine execution. It can be used
 * to carry state between coroutine execution steps by setting relations on it.
 * Each {@link CoroutineStep} receives the continuation of it's execution as an
 * argument to it's execution methods.
 *
 * @author eso
 */
public interface Continuation<T> extends UserDataHolderEx, CopyableUserDataHolder, Executor {

    /**
     * Awaits the completion of the coroutine execution. Other than
     * {@link #getResult()} this will not throw an exception on failure or
     * cancellation. An application must check these by itself if needed through
     * {@link #getError()} and {@link #isCancelled()}.
     */
    void await();

    /**
     * Awaits the completion of this continuation but only until a timeout is
     * reached. If the timeout is reached before completion the returned value
     * will be FALSE. Other than {@link #getResult(long, TimeUnit)} this will
     * not throw an exception on failure or cancellation. An application must
     * check these by itself if needed through {@link #getError()} and
     * {@link #isCancelled()}.
     *
     * @param timeout The timeout value
     * @param unit    The time unit of the the timeout
     * @return TRUE if the coroutine has finished, FALSE if the timeout elapsed
     */
    boolean await(long timeout, TimeUnit unit);

    /**
     * Cancels the execution of the associated {@link Coroutine} at the next
     * suspension point. Due to the nature of the cooperative concurrency of
     * coroutines there is no guarantee as to when the cancellation will occur.
     * The bMayInterruptIfRunning parameter is ignored because the thread on
     * which the current step is running is not known.
     */
    void cancel();

    /**
     * Finishes this continuation early with the given result, skipping any remaining steps in the coroutine chain.
     * Subsequent steps will be no-ops. The result will be available via {@link #getResult()} and {@link #toFuture()}.
     *
     * @param result The result value to finish with
     */
    void finishEarly(@Nullable T result);

    /**
     * Returns the context of the executed coroutine.
     *
     * @return The coroutine context
     */
    CoroutineContext context();

    /**
     * Continues the execution of a {@link CompletableFuture} by consuming it's
     * result. This is typically done by suspending steps that resume the
     * coroutine execution later. Coroutine steps must always invoke a continue
     * method to progress to a subsequent step and not use the methods of the
     * future directly.
     *
     * @param rPreviousExecution The future to continue
     * @param fNext              The next code to execute
     */
    <V extends @Nullable Object> void continueAccept(CompletableFuture<V> rPreviousExecution, Consumer<V> fNext);

    /**
     * Continues the execution of a {@link CompletableFuture} by applying a
     * function to it's result and then either invoking the next step in a chain
     * or finish the execution. Coroutine steps must always invoke a continue
     * method to progress to a subsequent step and not use the methods of the
     * future directly.
     *
     * @param previousExecution The future to continue
     * @param next              The next code to execute
     * @param nextStep          The next step
     */
    <I, O> void continueApply(
        CompletableFuture<I> previousExecution,
        Function<I, @Nullable O> next,
        @Nullable CoroutineStep<O, ?> nextStep
    );

    /**
     * Continues the execution of a {@link CompletableFuture} by composing it with another
     * future produced from its result, and then either invoking the next step in a chain
     * or finishing the execution. Coroutine steps must always invoke a continue method to
     * progress to a subsequent step and not use the methods of the future directly.
     *
     * @param previousExecution The future to continue
     * @param next              The function producing the future to await
     * @param nextStep          The next step
     */
    <I, O> void continueCompose(
        CompletableFuture<I> previousExecution,
        Function<I, CompletableFuture<O>> next,
        @Nullable CoroutineStep<O, ?> nextStep
    );

    /**
     * Marks an error of this continuation as handled. This will remove this
     * instance from the failed continuations of the scope and thus prevent the
     * scope from throwing an exception because of this error upon completion.
     *
     * @throws IllegalStateException If this instance has no error
     */
    void errorHandled();

    /**
     * Signals that an error occurred during the coroutine execution. This will
     * set this continuation to canceled and makes the error exception available
     * through {@link #getError()}. It will also invoke
     * {@link CoroutineScope#fail(Continuation)} on the scope this continuation
     * runs in.
     *
     * @param error The exception that caused the error
     * @return Always NULL but declared with a generic type so that it can be
     * used directly as a functional argument to
     * {@link CompletableFuture#exceptionally(Function)}
     */
    @Nullable <O> O fail(Throwable error);

    /**
     * Duplicated here for easier access during coroutine execution.
     *
     * @see CoroutineScope#getChannel(ChannelId)
     */
    <C> Channel<C> getChannel(ChannelId<C> id);

    /**
     * Returns a configuration value with a default value of NULL.
     *
     * @see #getConfiguration(Key, Object)
     */
    @Nullable <V> V getConfiguration(Key<V> configType);

    /**
     * Returns the value of a configuration relation. The lookup has the
     * precedence <i>continuation (this) -&gt; scope -&gt; context -&gt;
     * coroutine</i>, meaning that a configuration in an earlier stage overrides
     * the later ones. This means that a (static) configuration in a coroutine
     * definition can be overridden by the runtime stages.
     *
     * <p>
     * Coroutine steps that want to modify the configuration of the root
     * coroutine they are running in should set the configuration value on the
     * the continuation. To limit the change to the currently running coroutine
     * (e.g. a subroutine) configurations should be set on
     * {@link Continuation#getCurrentCoroutine()} instead.
     * </p>
     *
     * @param configType   The configuraton relation type
     * @param defaultValue The default value if no state relation exists
     * @return The configuration value (may be NULL)
     */
    @Nullable <V> V getConfiguration(Key<V> configType, @Nullable V defaultValue);

    /**
     * Returns either the root coroutine or, if subroutines have been started
     * from it, the currently executing subroutine.
     *
     * @return The currently executing coroutine
     */
    Coroutine<?, ?> getCurrentCoroutine();

    /**
     * Returns the current suspension.
     *
     * @return The current suspension or NULL for none
     */
    @Nullable Suspension<?> getCurrentSuspension();

    /**
     * Returns the error exception that caused a coroutine cancelation.
     *
     * @return The error or NULL for none
     */
    @Nullable Throwable getError();

    /**
     * Return the result of the coroutine execution. If this continuation has
     * been cancelled a {@link CancellationException} will be thrown. If it has
     * failed with an error a {@link CoroutineException} will be thrown.
     *
     * @return The result
     */
    T getResult();

    /**
     * Return the result of the coroutine execution or throws a
     * {@link CoroutineException} if a timeout is reached. If this continuation
     * has been cancelled a {@link CancellationException} will be thrown. If it
     * has failed with an error a {@link CoroutineException} will be thrown.
     *
     * @param timeout The timeout value
     * @param unit    The time unit of the the timeout
     * @return The result The result of the execution
     * @throws CoroutineException    If the timeout has elapsed before finishing
     *                               or an error occurred
     * @throws CancellationException If the coroutine had been cancelled
     */
    @Nullable T getResult(long timeout, TimeUnit unit);

    /**
     * Returns a state value with a default value of NULL.
     *
     * @see #getState(Key, Object)
     */
    @Nullable <V> V getState(Key<V> stateType);

    /**
     * Returns the value of a runtime state relation of the current execution.
     * This will first look for the value in currently executing coroutine
     * (either the root or a subroutine). If not found there the value will be
     * queried from this continuation first and if not there too, from the
     * scope. To the a runtime state value the respective relation needs to be
     * set on the appropriate stage (coroutine, continuation, scope).
     *
     * @param stateType    The state relation type
     * @param defaultValue The default value if no state relation exists
     * @return The runtime state value (may be null)
     */
    @Nullable <V> V getState(Key<V> stateType, @Nullable V defaultValue);

    /**
     * Returns the unique ID of this instance.
     *
     * @return The continuation ID
     */
    long id();

    /**
     * Checks if the execution of the coroutine has been cancelled. If it has
     * been cancelled because of and error the method {@link #getError()} will
     * return an exception.
     *
     * @return TRUE if the execution has been cancelled
     */
    boolean isCancelled();

    /**
     * Checks if the execution of the coroutine has finished. Whether it has
     * finished successfully or by cancelation can be checked with
     * {@link #isCancelled()}. If it has been cancelled because of and error the
     * method {@link #getError()} will return an exception.
     *
     * @return TRUE if the execution has finished
     */
    boolean isFinished();

    /**
     * Sets a function to be run if the execution of this instance is
     * cancelled.
     *
     * @param runOnCancel A function to be run on cancellation
     * @return This instance to allow additional invocations
     */
    Continuation<T> onCancel(Consumer<Continuation<T>> runOnCancel);

    /**
     * Sets a function to be run if the execution of this instance fails.
     *
     * @param runOnError A function to be run on cancellation
     * @return This instance to allow additional invocations
     */
    Continuation<T> onError(Consumer<Continuation<T>> runOnError);

    /**
     * Sets a function that will be invoked after the coroutine has successfully
     * finished execution and {@link #getResult()} is available. If the
     * execution of the coroutine is cancelled (by invoking {@link #cancel()})
     * the code will not be invoked. The code will be run directly, not
     * asynchronously.
     *
     * @param runWhenDone The consumer to process this continuation with when
     *                    the execution has finished
     * @return This instance to allow additional invocations
     */
    Continuation<T> onFinish(Consumer<Continuation<T>> runWhenDone);

    /**
     * Converts this continuation into a {@link CompletableFuture} that
     * completes when this continuation finishes execution.
     *
     * @return A completable future that will be completed with the result
     * of the coroutine execution, cancelled if the coroutine is
     * cancelled, or completed exceptionally if the coroutine fails
     */
    CompletableFuture<T> toFuture();

    /**
     * Returns the scope in which the coroutine is executed.
     *
     * @return The coroutine scope
     */
    CoroutineScope scope();

    /**
     * Suspends an invoking step for later invocation. Returns an instance of
     * {@link Suspension} that contains the state necessary for resuming the
     * execution. The suspension will not contain an input value because it is
     * typically not know upon suspension. It must be provided later, either
     * when resuming with {@link Suspension#resume(Object)} or by setting it
     * into the suspension with {@link Suspension#withValue(Object)}.
     *
     * @param suspendingStep The step initiating the suspension
     * @param suspendedStep  The step to suspend
     * @return A new suspension object
     */
    <V extends @Nullable Object> Suspension<V> suspend(CoroutineStep<?, V> suspendingStep, @Nullable CoroutineStep<V, ?> suspendedStep);

    /**
     * Suspends an invoking step for later invocation with the given instance of
     * a suspension subclass. This method is only intended for special
     * suspension cases. Most step implementations should call
     * {@link #suspend(CoroutineStep, CoroutineStep)} instead.
     *
     * @param suspension The suspension to suspend to
     * @return The suspension object
     */
    <V> Suspension<V> suspendTo(Suspension<V> suspension);
}
