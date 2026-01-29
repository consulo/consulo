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

import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * This is the base class for all steps of coroutines. For simple steps it is
 * sufficient to implement the single abstract method
 * {@link #execute(Object, Continuation)} which must perform the actual
 * (blocking) code execution. The default implementations of
 * {@link #runBlocking(Object, Continuation)} and
 * {@link #runAsync(CompletableFuture, CoroutineStep, Continuation)} then invoke
 * this method as needed.
 *
 * <p>
 * In most cases it is not necessary to extend this class because the 'step'
 * sub-package already contains implementations of several common steps. For
 * example, a simple code execution can be achieved by wrapping a closure in an
 * instance of the {@link CodeExecution} step.
 * </p>
 *
 * <p>
 * Creating a new step subclass is only needed to implement advanced coroutine
 * suspensions that are not already provided by existing steps. In such a case
 * it is typically also necessary to override the method
 * {@link #runAsync(CompletableFuture, CoroutineStep, Continuation)} to check
 * for the suspension condition. If a suspension is necessary a
 * {@link Suspension} object can be created by invoking
 * {@link Continuation#suspend(CoroutineStep, CoroutineStep)} for the current
 * step. The suspension object can then be used by code that waits for some
 * external condition to resume the coroutine when appropriate.
 * </p>
 *
 * <p>
 * It is recommended that a step implementation provides one or more static
 * factory methods alongside the constructor(s). These factory methods can then
 * be used as static imports for the fluent builder API of coroutines.
 * </p>
 *
 * @author eso
 */
public abstract class CoroutineStep<I, O> extends UserDataHolderBase {
    private String name = getClass().getSimpleName();

    /**
     * Creates a new instance.
     */
    protected CoroutineStep() {
    }

    public String getName() {
        return name;
    }

    public CoroutineStep<I, O> withName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    /**
     * Runs this execution step asynchronously as a continuation of a previous
     * code execution in a {@link CompletableFuture} and proceeds to the next
     * step afterwards.
     *
     * <p>
     * Subclasses that need to suspend the invocation of the next step until
     * some condition is met (e.g. sending or receiving data has finished) need
     * to override this method and create a {@link Suspension} by invoking
     * {@link Continuation#suspend(CoroutineStep, CoroutineStep)} on the next
     * step. If the condition that caused the suspension resolves the coroutine
     * execution can be resumed by calling {@link Suspension#resume(Object)}.
     * </p>
     *
     * <p>
     * Subclasses that override this method also need to handle errors by
     * terminating any further execution (i.e. not resuming a suspension if such
     * exists) and forwarding the causing exception to
     * {@link Continuation#fail(Throwable)}.
     * </p>
     *
     * @param previousExecution The future of the previous code execution
     * @param nextStep          The next step to execute or NULL for none
     * @param continuation      The continuation of the execution
     */
    public void runAsync(CompletableFuture<I> previousExecution,
                         CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        continuation.continueApply(previousExecution,
            i -> execute(i, continuation), nextStep);
    }

    /**
     * Runs this execution immediately, blocking the current thread until the
     * execution finishes.
     *
     * @param input        The input value
     * @param continuation The continuation of the execution
     * @return The execution result
     */
    public O runBlocking(I input, Continuation<?> continuation) {
        return execute(input, continuation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * This method must be implemented by subclasses to provide the actual
     * functionality of this step.
     *
     * @param input        The input value
     * @param continuation The continuation of the execution
     * @return The result of the execution
     */
    protected abstract O execute(I input, Continuation<?> continuation);

    /**
     * Allow subclasses to terminate the coroutine they currently run in.
     *
     * @param continuation The continuation of the current execution
     */
    protected void terminateCoroutine(Continuation<?> continuation) {
        continuation.getCurrentCoroutine().terminate(continuation);
    }
}
