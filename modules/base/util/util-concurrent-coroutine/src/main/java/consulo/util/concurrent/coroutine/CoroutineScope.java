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

import consulo.util.concurrent.coroutine.internal.RunLock;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static consulo.util.concurrent.coroutine.internal.Coroutines.EXCEPTION_HANDLER;
import static consulo.util.concurrent.coroutine.internal.Coroutines.closeManagedResources;

/**
 * A scope that manages one or more running coroutines. A new scope is created
 * through the factory method {@link #launch(CoroutineContext, ScopeCode)} It
 * executes an instance of the functional interface {@link ScopeCode} and blocks
 * the invoking thread until the code and all coroutines by it have finished
 * execution (either successfully or with an exception).
 *
 * <p>
 * An alternative way to launch a scope is by using the method
 * {@link #produce(CoroutineContext, Function, ScopeCode)}. It also executes the
 * given code (which in turn may start coroutines) but returns immediately after
 * the code finished with a {@link ScopeFuture} instance. This sub-interface of
 * {@link Future} can then be used to wait for the started coroutines to finish
 * or to cancel the execution. As the name indicates, this method is mainly
 * intended for scope executions that produce result. But it can also be used to
 * just wrap a scope execution to handle it as a future and then ignore the
 * result.
 * </p>
 *
 * <p>
 * A scope will also automatically close all ({@link AutoCloseable}) resources
 * that are stored in it with relations that have the annotation
 * {@link MetaTypes#MANAGED}.
 * </p>
 *
 * @author eso
 */
public class CoroutineScope extends CoroutineEnvironment {

    private final CoroutineContext context;

    private final AtomicLong runningCoroutines = new AtomicLong();

    private final RunLock scopeLock = new RunLock();

    private final Collection<Suspension<?>> suspensions = new LinkedHashSet<>();

    private final SequencedSet<Continuation<?>> failedContinuations =
        new LinkedHashSet<>();

    private CountDownLatch finishSignal = new CountDownLatch(1);

    private boolean cancelOnError = true;

    private boolean cancelled = false;

    /**
     * Creates a new instance.
     *
     * @param context The context to run the scope's coroutines in
     */
    CoroutineScope(@Nonnull CoroutineContext context) {
        this.context = Objects.requireNonNull(context, () -> "CoroutineContext required");
        this.context.scopeLaunched(this);
    }

    /**
     * Launches a new scope for the execution of coroutine in a specific
     * context. This method will block the invoking thread until all coroutines
     * launched by the argument builder have terminated, either successfully, by
     * cancellation, or with errors.
     *
     * <p>
     * If one or more of the coroutines or the scope code throw an exception
     * this method will throw a {@link CoroutineScopeException} as soon as all
     * other coroutines have terminated. By default an error causes all other
     * coroutines to be cancelled but that can be changed with
     * {@link #setCancelOnError(boolean)}. If any other coroutines fail after
     * the first error their continuations will also be added to the exception.
     * </p>
     *
     * @param context The coroutine context for the scope
     * @param code    The code to execute in the scope
     * @throws CoroutineScopeException If one or more of the executed coroutines
     *                                 failed
     */
    public static void launch(CoroutineContext context, ScopeCode code) {
        CoroutineScope aScope = new CoroutineScope(context);

        try {
            code.runIn(aScope);

            aScope.await();
        }
        catch (Exception e) {
            // even on errors wait for all asynchronous invocations
            // in the scope to finish
            aScope.await();
            throw new CoroutineScopeException(e, aScope.failedContinuations);
        }
        aScope.checkThrowErrors();
    }

    /**
     * Launches a new scope that is expected to produce a result and returns a
     * {@link Future} instance that can be used to query the result. The result
     * will be retrieved after the coroutine execution finished from the scope
     * by applying the result function. If the future object is only needed to
     * wrap a scope execution this function
     *
     * @param context   The coroutine context for the scope
     * @param getResult A function that retrieves the result from the scope or
     *                  NULL to always return NULL
     * @param code      The producing code to execute in the scope
     * @return A future that provides access to the result of the scope
     * execution
     */
    public static <T> ScopeFuture<T> produce(CoroutineContext context,
                                             Function<? super CoroutineScope, T> getResult, ScopeCode code) {
        return new ScopeFuture<>(new CoroutineScope(context), getResult, code);
    }

    /**
     * Blocks until all coroutines in this scope have finished execution. If no
     * coroutines are running or all have finished execution already this method
     * returns immediately.
     */
    public void await() {
        try {
            if (getCoroutineCount() > 0) {
                finishSignal.await();
            }
        }
        catch (Exception e) {
            throw new CoroutineException(e);
        }
        finally {
            context.scopeFinished(this);
            closeManagedResources(this, getCopyableUserData(EXCEPTION_HANDLER));
        }
    }

    /**
     * Blocks until all coroutines in this scope have finished execution or a
     * timeout expires. If the timeout is reached this method will return but
     * the scope will continue to execute. If necessary it can be cancelled by
     * calling {@link #cancel()}.
     *
     * @param timeout The maximum time to wait
     * @param unit    The unit of the timeout
     * @return TRUE if the scope has finished execution; FALSE if the timeout
     * was reached
     */
    public boolean await(long timeout, TimeUnit unit) {
        boolean completed;

        try {
            completed = finishSignal.await(timeout, unit);
        }
        catch (Exception e) {
            throw new CoroutineException(e);
        }
        finally {
            context.scopeFinished(this);
            closeManagedResources(this, getCopyableUserData(EXCEPTION_HANDLER));
        }
        return completed;
    }

    /**
     * Cancels the execution of all coroutines that are currently running in
     * this scope.
     */
    public void cancel() {
        scopeLock.runLocked(() -> {
            if (!isFinished()) {
                cancelled = true;

                for (Suspension<?> rSuspension : suspensions) {
                    rSuspension.cancel();
                }
                suspensions.clear();
            }
        });
    }

    /**
     * Returns the context in which coroutines of this scope are executed.
     *
     * @return The coroutine context
     */
    public CoroutineContext context() {
        return context;
    }

    /**
     * Checks this scope and the {@link CoroutineContext} for a channel with the
     * given ID. If no such channel exists it will be created in this scope. If
     * a context channel is needed instead it needs to be created in advance
     * with {@link CoroutineContext#createChannel(ChannelId, int)}.
     *
     * @see CoroutineEnvironment#getChannel(ChannelId)
     */
    @Override
    public <T> Channel<T> getChannel(ChannelId<T> id) {
        if (context.hasChannel(id)) {
            return context.getChannel(id);
        }
        else {
            return super.getChannel(id);
        }
    }

    /**
     * Returns the number of currently running coroutines. This will only be a
     * momentary value as the execution of the coroutines happens asynchronously
     * and coroutines may finish while querying this count.
     *
     * @return The number of running coroutines
     */
    public long getCoroutineCount() {
        return runningCoroutines.get();
    }

    /**
     * Checks whether a channel with the given ID exists in this scope or in the
     * {@link CoroutineContext}.
     *
     * @see CoroutineEnvironment#hasChannel(ChannelId)
     */
    @Override
    public boolean hasChannel(ChannelId<?> id) {
        return super.hasChannel(id) || context.hasChannel(id);
    }

    /**
     * Checks whether the execution of the other coroutines in this scope is
     * canceled if an exception occurs in a coroutine. Can be changed with
     * {@link #setCancelOnError(boolean)}.
     *
     * @return TRUE if all coroutines are cancelled if a coroutine fails
     */
    public boolean isCancelOnError() {
        return cancelOnError;
    }

    /**
     * Checks whether this scope has been cancelled.
     *
     * @return TRUE if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Non-blockingly checks whether this scope has finished execution of all
     * coroutines. Due to the asynchronous nature of coroutine executions this
     * method will only return when preceded by a blocking call like
     * {@link #await()}.
     *
     * @return TRUE if finished
     */
    public boolean isFinished() {
        return finishSignal.getCount() == 0;
    }

    /**
     * Removes a channel from this scope or from the {@link CoroutineContext}.
     * If it exists in both it will only be removed from this scope.
     *
     * @see CoroutineEnvironment#removeChannel(ChannelId)
     */
    @Override
    public void removeChannel(ChannelId<?> id) {
        if (hasChannel(id)) {
            super.removeChannel(id);
        }
        else {
            context.removeChannel(id);
        }
    }

    /**
     * Sets the behavior on coroutine errors in the scope. If set to TRUE (which
     * is the default) any exception in a coroutine will cancel the execution of
     * this scope. If FALSE all other coroutines are allowed to finish execution
     * (or fail too) before the scope's execution is finished. In any case the
     * scope will throw a {@link CoroutineScopeException} if one or more errors
     * occurred.
     *
     * @param cancelOnError TRUE to cancel running coroutine if an error occurs;
     *                      FALSE to let them finish
     */
    public void setCancelOnError(boolean cancelOnError) {
        this.cancelOnError = cancelOnError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s[%d]", getClass().getSimpleName(),
            runningCoroutines.longValue());
    }

    /**
     * Adds a suspension of a coroutine in this scope.
     *
     * @param suspension The suspension to add
     */
    void addSuspension(Suspension<?> suspension) {
        scopeLock.runLocked(() -> {
            if (cancelled) {
                suspension.cancel();
            }
            else {
                suspensions.add(suspension);
            }
        });
    }

    /**
     * Throws an exception if errors occurred during the coroutine executions in
     * this scope.
     */
    void checkThrowErrors() {
        if (failedContinuations.size() > 0) {
            if (failedContinuations.size() == 1) {
                Throwable eError = failedContinuations.getFirst().getError();

                if (eError instanceof CoroutineException) {
                    throw (CoroutineException) eError;
                }
                else {
                    throw new CoroutineScopeException(failedContinuations);
                }
            }
            else {
                throw new CoroutineScopeException(failedContinuations);
            }
        }
    }

    /**
     * Removes a continuation from the list of failed continuations to prevent
     * an error exception upon completion.
     *
     * @param continuation The continuation to remove
     */
    void continuationErrorHandled(Continuation<?> continuation) {
        failedContinuations.remove(continuation);
    }

    /**
     * Notifies this context that a coroutine execution has been finished
     * (either regularly or by canceling).
     *
     * @param continuation The continuation of the execution
     */
    void coroutineFinished(Continuation<?> continuation) {
        if (runningCoroutines.decrementAndGet() == 0) {
            finishSignal.countDown();
        }
    }

    /**
     * Notifies this context that a coroutine has been started in it.
     *
     * @param continuation The continuation of the execution
     */
    void coroutineStarted(Continuation<?> continuation) {
        if (runningCoroutines.incrementAndGet() == 1 && finishSignal.getCount() == 0) {
            finishSignal = new CountDownLatch(1);
        }
    }

    /**
     * Signals this scope that an error occurred during a certain coroutine
     * execution. This will cancel the execution of all coroutines in this scope
     * and throw a {@link CoroutineScopeException} from the
     * {@link #launch(ScopeCode)} methods.
     *
     * @param continuation The continuation that failed with an exception
     */
    void fail(Continuation<?> continuation) {
        scopeLock.runLocked(() -> {
            failedContinuations.add(continuation);

            if (cancelOnError && !cancelled) {
                cancel();
            }
            coroutineFinished(continuation);
        });
    }

    /**
     * Adds a suspension of a coroutine in this scope.
     *
     * @param suspension The suspension to add
     */
    void removeSuspension(Suspension<?> suspension) {
        scopeLock.runLocked(() -> {
            if (!cancelled) {
                suspensions.remove(suspension);
            }
        });
    }

    /**
     * A functional interface that will be executed in a scope that has been
     * launched with {@link CoroutineScope#launch(ScopeCode)}. It is typically
     * used in the form of a lambda expression or method reference.
     *
     * @author eso
     */
    @FunctionalInterface
    public interface ScopeCode {

        /**
         * Starts coroutines in the given {@link CoroutineScope} by invoking
         * methods like {@link Coroutine#runAsync(CoroutineScope, Object)} on it
         * and optionally also performs other operations, like processing the
         * results.
         *
         * @param rScope The scope to run in
         * @throws Exception Executions may throw arbitrary exceptions which
         *                   will be handled by the scope
         */
        void runIn(CoroutineScope rScope) throws Exception;
    }

    /**
     * An implementation of the future interface that wraps a scope execution.
     *
     * @author eso
     */
    public static class ScopeFuture<T> implements Future<T> {

        private final CoroutineScope scope;

        private final Function<? super CoroutineScope, T> getResult;

        private Exception scopeCodeError;

        /**
         * Creates a new instance for a certain scope.
         *
         * @param scope     The scope to await for the result
         * @param getResult A function that retrieves the result from the scope
         *                  or NULL to always return NULL
         * @param code      The code to be executed in the scope
         */
        public ScopeFuture(CoroutineScope scope,
                           Function<? super CoroutineScope, T> getResult, ScopeCode code) {
            this.scope = scope;
            this.getResult = getResult;

            try {
                code.runIn(scope);
            }
            catch (Exception e) {
                scopeCodeError = e;
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean terminated = scope.isFinished() || scope.isCancelled();

            if (!terminated) {
                scope.cancel();
                terminated = true;
            }
            return terminated;
        }

        @Override
        public T get() {
            scope.await();

            return getImpl();
        }

        @Override
        public T get(long timeout, TimeUnit unit) {
            scope.await(timeout, unit);

            return getImpl();
        }

        @Override
        public boolean isCancelled() {
            return scope.isCancelled();
        }

        @Override
        public boolean isDone() {
            return scope.isFinished();
        }

        /**
         * Implements getting the scope result (without awaiting).
         *
         * @return The scope result
         */
        private T getImpl() {
            if (scopeCodeError != null) {
                throw new CoroutineScopeException(scopeCodeError,
                    scope.failedContinuations);
            }
            scope.checkThrowErrors();

            if (isCancelled()) {
                throw new CancellationException("Scope is cancelled");
            }
            return getResult != null ? getResult.apply(scope) : null;
        }
    }
}
