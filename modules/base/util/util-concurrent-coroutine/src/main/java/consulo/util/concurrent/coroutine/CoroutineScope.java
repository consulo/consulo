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

import consulo.util.concurrent.coroutine.internal.CoroutineScopeImpl;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

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
 * the code finished with a {@link Future} instance. This can then be used to
 * wait for the started coroutines to finish or to cancel the execution. As the
 * name indicates, this method is mainly intended for scope executions that
 * produce result. But it can also be used to just wrap a scope execution to
 * handle it as a future and then ignore the result.
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
public interface CoroutineScope extends CoroutineEnvironment {

    /**
     * Creates a new scope for the execution of coroutines in a specific
     * context.
     *
     * @param context The context to run the scope's coroutines in
     * @return The new scope instance
     */
    static CoroutineScope of(CoroutineContext context) {
        return new CoroutineScopeImpl(context);
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
    static void launch(CoroutineContext context, ScopeCode code) {
        CoroutineScopeImpl.launch(context, code);
    }

    static void launchAsync(CoroutineContext context, Supplier<Coroutine<@Nullable ?, ?>> supplier) {
        CoroutineScopeImpl.launchAsync(context, supplier);
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
    static <T> Future<T> produce(CoroutineContext context,
                                 Function<? super CoroutineScope, T> getResult, ScopeCode code) {
        return CoroutineScopeImpl.produce(context, getResult, code);
    }

    /**
     * Blocks until all coroutines in this scope have finished execution. If no
     * coroutines are running or all have finished execution already this method
     * returns immediately.
     */
    void await();

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
    boolean await(long timeout, TimeUnit unit);

    /**
     * Cancels the execution of all coroutines that are currently running in
     * this scope.
     */
    void cancel();

    /**
     * Returns the context in which coroutines of this scope are executed.
     *
     * @return The coroutine context
     */
    CoroutineContext context();

    /**
     * Returns the number of currently running coroutines. This will only be a
     * momentary value as the execution of the coroutines happens asynchronously
     * and coroutines may finish while querying this count.
     *
     * @return The number of running coroutines
     */
    long getCoroutineCount();

    /**
     * Checks whether the execution of the other coroutines in this scope is
     * canceled if an exception occurs in a coroutine. Can be changed with
     * {@link #setCancelOnError(boolean)}.
     *
     * @return TRUE if all coroutines are cancelled if a coroutine fails
     */
    boolean isCancelOnError();

    /**
     * Checks whether this scope has been cancelled.
     *
     * @return TRUE if cancelled
     */
    boolean isCancelled();

    /**
     * Non-blockingly checks whether this scope has finished execution of all
     * coroutines. Due to the asynchronous nature of coroutine executions this
     * method will only return when preceded by a blocking call like
     * {@link #await()}.
     *
     * @return TRUE if finished
     */
    boolean isFinished();

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
    void setCancelOnError(boolean cancelOnError);

    /**
     * Throws an exception if errors occurred during the coroutine executions in
     * this scope.
     */
    void checkThrowErrors();

    /**
     * A functional interface that will be executed in a scope that has been
     * launched with {@link CoroutineScope#launch(CoroutineContext, ScopeCode)}. It is
     * typically used in the form of a lambda expression or method reference.
     *
     * @author eso
     */
    @FunctionalInterface
    interface ScopeCode {

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
}
