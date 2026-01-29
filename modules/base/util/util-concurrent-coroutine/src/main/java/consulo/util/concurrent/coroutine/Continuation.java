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

import consulo.util.concurrent.coroutine.CoroutineEvent.EventType;
import consulo.util.concurrent.coroutine.internal.Coroutines;
import consulo.util.concurrent.coroutine.internal.DefaultExceptionHandler;
import consulo.util.concurrent.coroutine.internal.RunLock;
import consulo.util.dataholder.CopyableUserDataHolder;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.util.concurrent.coroutine.internal.Coroutines.*;

/**
 * A continuation represents the state of a coroutine execution. It can be used
 * to carry state between coroutine execution steps by setting relations on it.
 * Each {@link CoroutineStep} receives the continuation of it's execution as an
 * argument to it's execution methods.
 *
 * @author eso
 */
public class Continuation<T> extends UserDataHolderBase implements Executor {

    private static final AtomicLong aNextId = new AtomicLong(1);

    private final CoroutineScope scope;

    private final long id = aNextId.getAndIncrement();

    private final Deque<Coroutine<?, ?>> coroutineStack = new ArrayDeque<>();

    private final CountDownLatch finishSignal = new CountDownLatch(1);

    private final RunLock stateLock = new RunLock();

    BiConsumer<Suspension<?>, Boolean> suspensionListener = null;

    BiConsumer<CoroutineStep<?, ?>, Continuation<?>> fStepListener = null;

    private T result = null;

    private boolean callChainComplete = false;

    private boolean cancelled = false;

    private boolean finished = false;

    private Throwable error = null;

    private CompletableFuture<?> currentExecution = null;

    private Suspension<?> currentSuspension = null;

    private Consumer<Continuation<T>> runWhenDone;

    private Consumer<Continuation<T>> runOnCancel;

    private Consumer<Continuation<T>> runOnError;

    /**
     * Creates a new instance for the execution of the given {@link Coroutine}
     * in a certain scope.
     *
     * @param scope     The coroutine context
     * @param coroutine The coroutine that is executed with this continuation
     */
    public Continuation(CoroutineScope scope, Coroutine<?, T> coroutine) {
        this.scope = scope;

        coroutineStack.push(coroutine);

        suspensionListener = getConfiguration(COROUTINE_SUSPENSION_LISTENER);
        fStepListener = getConfiguration(COROUTINE_STEP_LISTENER);

        scope.coroutineStarted(this);
        notifyListeners(EventType.STARTED);
    }

    /**
     * Awaits the completion of the coroutine execution. Other than
     * {@link #getResult()} this will not throw an exception on failure or
     * cancellation. An application must check these by itself if needed through
     * {@link #getError()} and {@link #isCancelled()}.
     */
    public void await() {
        try {
            finishSignal.await();
        }
        catch (InterruptedException e) {
            throw new CoroutineException(e);
        }
    }

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
    public boolean await(long timeout, TimeUnit unit) {
        try {
            return finishSignal.await(timeout, unit);
        }
        catch (InterruptedException e) {
            throw new CoroutineException(e);
        }
    }

    /**
     * Cancels the execution of the associated {@link Coroutine} at the next
     * suspension point. Due to the nature of the cooperative concurrency of
     * coroutines there is no guarantee as to when the cancellation will occur.
     * The bMayInterruptIfRunning parameter is ignored because the thread on
     * which the current step is running is not known.
     */
    public void cancel() {
        stateLock.runLocked(() -> {
            if (!finished) {
                cancelled = true;
                finish(null);

                if (runOnCancel != null) {
                    runOnCancel.accept(this);
                }
            }
        });

        if (currentSuspension != null) {
            currentSuspension.cancel();
        }
        if (callChainComplete && currentExecution != null) {
            // only cancel last CompletableFuture in a chain to avoid
            // state-locking for each step execution which would be necessary
            // as canceling typically occurs from a separate thread
            currentExecution.cancel(false);
        }
    }

    /**
     * Returns the context of the executed coroutine.
     *
     * @return The coroutine context
     */
    public final CoroutineContext context() {
        return scope.context();
    }

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
    public final <V> void continueAccept(
        CompletableFuture<V> rPreviousExecution, Consumer<V> fNext) {
        if (!cancelled) {
            currentExecution = rPreviousExecution.thenAcceptAsync(fNext, this)
                .exceptionally(this::fail);
        }
        else if (currentExecution != null) {
            currentExecution.cancel(false);
        }
    }

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
    public final <I, O> void continueApply(
        CompletableFuture<I> previousExecution, Function<I, O> next,
        CoroutineStep<O, ?> nextStep) {
        if (!cancelled) {
            CompletableFuture<O> rNextExecution =
                previousExecution.thenApplyAsync(next, this);

            currentExecution = rNextExecution;

            if (nextStep != null) {
                // the next step is either a StepChain which contains it's own
                // next step or the final step in a coroutine and therefore the
                // rNextStep argument can be NULL
                nextStep.runAsync(rNextExecution, null, this);
            }
            else {
                // only add exception handler to the end of a chain, i.e. next == null
                rNextExecution.exceptionally(this::fail);

                // and signal that no more steps will be executed
                callChainComplete = true;
            }
        }
        else if (currentExecution != null) {
            currentExecution.cancel(false);
        }
    }

    /**
     * Marks an error of this continuation as handled. This will remove this
     * instance from the failed continuations of the scope and thus prevent the
     * scope from throwing an exception because of this error upon completion.
     *
     * @throws IllegalStateException If this instance has no error
     */
    public void errorHandled() {
        if (error == null) {
            throw new IllegalStateException("No error exists");
        }
        scope.continuationErrorHandled(this);
    }

    /**
     * Forwards the execution to the executor of the {@link CoroutineContext}.
     *
     * @see Executor#execute(Runnable)
     */
    @Override
    public void execute(Runnable command) {
        context().getExecutor().execute(command);
    }

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
    public <O> O fail(Throwable error) {
        if (!finished) {
            this.error = error;
            scope.fail(this);
            cancel();

            getConfiguration(EXCEPTION_HANDLER, DefaultExceptionHandler.INSTANCE).accept(error);

            if (runOnError != null) {
                runOnError.accept(this);
            }
        }
        return null;
    }

    /**
     * Duplicated here for easier access during coroutine execution.
     *
     * @see CoroutineScope#getChannel(ChannelId)
     */
    public final <C> Channel<C> getChannel(ChannelId<C> id) {
        return scope.getChannel(id);
    }

    /**
     * Returns a configuration value with a default value of NULL.
     *
     * @see #getConfiguration(RelationType, Object)
     */
    public <V> V getConfiguration(Key<V> configType) {
        return getConfiguration(configType, null);
    }

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
    public <V> V getConfiguration(Key<V> configType, V defaultValue) {
        V data = getCopyableUserData(configType);
        if (data != null) {
            return data;
        }

        data = scope.getCopyableUserData(configType);
        if (data != null) {
            return data;
        }

        data = scope.context().getCopyableUserData(configType);
        if (data != null) {
            return data;
        }

        Coroutine<?, ?> rCoroutine = getCurrentCoroutine();

        // if rDefault is NULL always query the relation to also get
        // default and initial values
        if (defaultValue == null) {
            data = rCoroutine.getUserData(configType);
            if (data != null) {
                return data;
            }
        }
        return defaultValue;
    }

    /**
     * Returns either the root coroutine or, if subroutines have been started
     * from it, the currently executing subroutine.
     *
     * @return The currently executing coroutine
     */
    public final Coroutine<?, ?> getCurrentCoroutine() {
        return coroutineStack.peek();
    }

    /**
     * Returns the current suspension.
     *
     * @return The current suspension or NULL for none
     */
    public final Suspension<?> getCurrentSuspension() {
        return currentSuspension;
    }

    /**
     * Returns the error exception that caused a coroutine cancelation.
     *
     * @return The error or NULL for none
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Return the result of the coroutine execution. If this continuation has
     * been cancelled a {@link CancellationException} will be thrown. If it has
     * failed with an error a {@link CoroutineException} will be thrown.
     *
     * @return The result
     */
    public T getResult() {
        try {
            finishSignal.await();
        }
        catch (InterruptedException e) {
            throw new CoroutineException(e);
        }
        return getResultImpl();
    }

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
    public T getResult(long timeout, TimeUnit unit) {
        try {
            if (!finishSignal.await(timeout, unit)) {
                throw new CoroutineException("Timeout reached");
            }
        }
        catch (InterruptedException e) {
            throw new CoroutineException(e);
        }
        return getResultImpl();
    }

    /**
     * Returns a state value with a default value of NULL.
     *
     * @see #getState(Key, Object)
     */
    public <V> V getState(Key<V> stateType) {
        return getState(stateType, null);
    }

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
    public <V> V getState(Key<V> stateType, V defaultValue) {
        Coroutine<?, ?> coroutine = getCurrentCoroutine();

        V data = coroutine.getUserData(stateType);
        if (data != null) {
            return data;
        }

        data = getUserData(stateType);
        if (data != null) {
            return data;
        }

        data = scope.getUserData(stateType);
        if (data != null) {
            return data;
        }

        return defaultValue;
    }

    /**
     * Returns the unique ID of this instance.
     *
     * @return The continuation ID
     */
    public final long id() {
        return id;
    }

    /**
     * Checks if the execution of the coroutine has been cancelled. If it has
     * been cancelled because of and error the method {@link #getError()} will
     * return an exception.
     *
     * @return TRUE if the execution has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Checks if the execution of the coroutine has finished. Whether it has
     * finished successfully or by cancelation can be checked with
     * {@link #isCancelled()}. If it has been cancelled because of and error the
     * method {@link #getError()} will return an exception.
     *
     * @return TRUE if the execution has finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Sets a function to be run if the execution of this instance is
     * cancelled.
     *
     * @param runOnCancel A function to be run on cancellation
     * @return This instance to allow additional invocations
     */
    public Continuation<T> onCancel(Consumer<Continuation<T>> runOnCancel) {
        // ensure that function is not set while cancel is in progress
        stateLock.runLocked(() -> {
            if (cancelled && error == null) {
                runOnCancel.accept(this);
            }
            else {
                this.runOnCancel = runOnCancel;
            }
        });

        return this;
    }

    /**
     * Sets a function to be run if the execution of this instance fails.
     *
     * @param runOnError A function to be run on cancellation
     * @return This instance to allow additional invocations
     */
    public Continuation<T> onError(Consumer<Continuation<T>> runOnError) {
        // ensure that function is not set while cancel is in progress
        stateLock.runLocked(() -> {
            if (cancelled && error != null) {
                runOnError.accept(this);
            }
            else {
                this.runOnError = runOnError;
            }
        });

        return this;
    }

    /**
     * Sets a function that will be invoked after the coroutine has successfully
     * finished execution and {@link #finish(Object)} has been invoked. If the
     * execution of the coroutine is cancelled (by invoking {@link #cancel()})
     * the code will not be invoked. The code will be run directly, not
     * asynchronously.
     *
     * @param runWhenDone The consumer to process this continuation with when
     *                    the execution has finished
     * @return This instance to allow additional invocations
     */
    public Continuation<T> onFinish(Consumer<Continuation<T>> runWhenDone) {
        // ensure that function is not set while finishing is in progress
        stateLock.runLocked(() -> {
            this.runWhenDone = runWhenDone;

            if (finished && !cancelled) {
                runWhenDone.accept(this);
            }
        });

        return this;
    }

    /**
     * Returns the scope in which the coroutine is executed.
     *
     * @return The coroutine scope
     */
    public final CoroutineScope scope() {
        return scope;
    }

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
    public <V> Suspension<V> suspend(CoroutineStep<?, V> suspendingStep,
                                     CoroutineStep<V, ?> suspendedStep) {
        return suspendTo(new Suspension<>(suspendingStep, suspendedStep, this));
    }

    /**
     * Suspends an invoking step for later invocation with the given instance of
     * a suspension subclass. This method is only intended for special
     * suspension cases. Most step implementations should call
     * {@link #suspend(CoroutineStep, CoroutineStep)} instead.
     *
     * @param suspension The suspension to suspend to
     * @return The suspension object
     */
    public <V> Suspension<V> suspendTo(Suspension<V> suspension) {
        // only one suspension per continuation is possible
        assert currentSuspension == null;

        scope.addSuspension(suspension);
        currentSuspension = suspension;
        currentExecution = null;

        if (suspensionListener != null) {
            suspensionListener.accept(currentSuspension, true);
        }
        return suspension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s-%d[%s]", getCurrentCoroutine().getName(), id,
            result);
    }

    /**
     * Signals a finished {@link Coroutine} execution. This is invoked
     * internally by the framework at the end of the execution.
     *
     * @param result The result of the coroutine execution
     */
    void finish(T result) {
        assert !finished;
        assert coroutineStack.size() == 1;

        try {
            this.result = result;

            // lock ensures that setting of fRunWhenDone is correctly synchronized
            stateLock.runLocked(() -> finished = true);

            finishSignal.countDown();

            scope.coroutineFinished(this);
            notifyListeners(EventType.FINISHED);

            if (!cancelled && runWhenDone != null) {
                runWhenDone.accept(this);
            }
        }
        finally {
            Consumer<Throwable> errorHandler = getConfiguration(EXCEPTION_HANDLER, DefaultExceptionHandler.INSTANCE);

            closeManagedResources(getCurrentCoroutine(), errorHandler);
            closeManagedResources(this, errorHandler);
        }
    }

    /**
     * Resumes the asynchronous execution of this coroutine at a certain step.
     *
     * @param resumeStep The step to resume execution at
     * @param value      The value to resume the step with
     */
    final <V> void resumeAsync(CoroutineStep<V, ?> resumeStep, V value) {
        if (!cancelled) {
            CompletableFuture<V> aResumeExecution =
                CompletableFuture.supplyAsync(() -> value, this);

            currentExecution = aResumeExecution;

            // the resume step is always either a StepChain which contains it's
            // own next step or the final step in a coroutine and therefore
            // rNextStep can be NULL
            resumeStep.runAsync(aResumeExecution, null, this);
        }
        else if (currentExecution != null) {
            currentExecution.cancel(false);
        }
    }

    /**
     * Removes a subroutine from the coroutines stack when it has finished
     * execution. This will also close all managed resources stored in the
     * coroutine.
     */
    void subroutineFinished() {
        closeManagedResources(getCurrentCoroutine(), getConfiguration(EXCEPTION_HANDLER, DefaultExceptionHandler.INSTANCE));

        coroutineStack.pop();
    }

    /**
     * Pushes a subroutine on the coroutines stack upon execution.
     *
     * @param subroutine The subroutine
     */
    void subroutineStarted(Subroutine<?, ?, ?> subroutine) {
        coroutineStack.push(subroutine);
    }

    /**
     * Gets notified by {@link Suspension#resume(Object)} upon resuming.
     *
     * @param suspension The suspension to resume
     */
    <I> void suspensionResumed(Suspension<I> suspension) {
        assert currentSuspension == suspension;

        if (!isCancelled()) {
            if (suspensionListener != null) {
                suspensionListener.accept(currentSuspension, false);
            }
        }
        currentSuspension = null;
    }

    /**
     * Traces the execution of coroutine steps (typically for debugging
     * purposes). Invokes the listener provided in the relation with the type
     * {@link Coroutines#COROUTINE_STEP_LISTENER} if it is not NULL.
     *
     * @param step The step to trace
     */
    final void trace(CoroutineStep<?, ?> step) {
        if (fStepListener != null) {
            fStepListener.accept(step, this);
        }
    }

    /**
     * Internal implementation of querying the the result.
     *
     * @return The result
     */
    private T getResultImpl() {
        if (cancelled) {
            if (error != null) {
                if (error instanceof CoroutineException) {
                    throw (CoroutineException) error;
                }
                else {
                    throw new CoroutineException(error);
                }
            }
            else {
                throw new CancellationException();
            }
        }
        return result;
    }

    /**
     * Notifies the coroutine listeners that are registered in the coroutine,
     * the scope, and the context.
     *
     * @param type The event type
     */
    private void notifyListeners(EventType type) {
        CopyableUserDataHolder[] rSources =
            new CopyableUserDataHolder[]{getCurrentCoroutine(), scope, scope.context()};

        CoroutineEvent event = new CoroutineEvent(this, type);
        for (CopyableUserDataHolder rSource : rSources) {
            List<Consumer<CoroutineEvent>> consumers = rSource.getCopyableUserData(COROUTINE_LISTENERS);
            if (consumers != null) {
                for (Consumer<CoroutineEvent> consumer : consumers) {
                    consumer.accept(event);
                }
            }
        }
    }
}
