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
package consulo.util.concurrent.coroutine.step;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static consulo.util.concurrent.coroutine.step.CodeExecution.consume;

/********************************************************************
 * A {@link Coroutine} step that loops over another step (which may be a
 * subroutine) as long as a predicate yields TRUE for the input value and/or the
 * current continuation. The looped step get's the loop input value as it's
 * input on the first iteration and must return a value of the same type which
 * will then be used to test the condition before the next loop run.
 *
 * <p>If more complex conditions need to be checked the loop condition can check
 * relations that have been set in the {@link Continuation} by the looped
 * step.</p>
 *
 * @author eso
 */
public class Loop<T> extends CoroutineStep<T, T> {
	//~ Instance fields --------------------------------------------------------

	private final BiPredicate<? super T, Continuation<?>> pCondition;

	private final CoroutineStep<T, T> rLoopedStep;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param fCondition  The condition to check for TRUE to continue looping
	 * @param rLoopedStep The step to execute in the loop
	 */
	public Loop(BiPredicate<? super T, Continuation<?>> fCondition,
		CoroutineStep<T, T> rLoopedStep) {
		this.pCondition = fCondition;
		this.rLoopedStep = rLoopedStep;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Repeatedly executes a certain step as long as the given condition is TRUE
	 * for the current value and the continuation of the execution. The current
	 * value is initialized from the loop input value and updated by the looped
	 * step on each iteration.
	 *
	 * @param  pCondition  The condition to check for TRUE to continue looping
	 * @param  rLoopedStep The step to execute in the loop
	 *
	 * @return A new step instance
	 */
	public static <T> Loop<T> loopWhile(
		BiPredicate<? super T, Continuation<?>> pCondition,
		CoroutineStep<T, T> rLoopedStep) {
		return new Loop<>(pCondition, rLoopedStep);
	}

	/***************************************
	 * Repeatedly executes a certain step as long as the given condition is TRUE
	 * for the current value. The current value is initialized from the loop
	 * input value and updated by the looped step on each iteration.
	 *
	 * <p>If more complex conditions need to be checked the method {@link
	 * #loopWhile(BiPredicate, CoroutineStep)} can be used to check relations
	 * that have been set in the {@link Continuation} by the looped step.</p>
	 *
	 * @param  pCondition  The condition to check for TRUE to continue looping
	 * @param  rLoopedStep The step to execute in the loop
	 *
	 * @return A new step instance
	 */
	public static <T> Loop<T> loopWhile(Predicate<T> pCondition,
		CoroutineStep<T, T> rLoopedStep) {
		return loopWhile((i, c) -> pCondition.test(i), rLoopedStep);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<T> previousExecution,
		CoroutineStep<T, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution,
			i -> loopAsync(i, nextStep, continuation));
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected T execute(T input, Continuation<?> continuation) {
		while (pCondition.test(input, continuation)) {
			input = rLoopedStep.runBlocking(input, continuation);
		}

		return input;
	}

	/***************************************
	 * Performs the actual looping by repeatedly invoking this method until the
	 * loop condition is FALSE.
	 *
	 * @param rInput        The input value to the loop
	 * @param rNextStep     The next step to be invoked when the condition is
	 *                      FALSE
	 * @param rContinuation The continuation of the execution
	 */
	private void loopAsync(T rInput, CoroutineStep<T, ?> rNextStep,
		Continuation<?> rContinuation) {
		if (pCondition.test(rInput, rContinuation)) {
			CompletableFuture<T> fLoopIteration =
				CompletableFuture.supplyAsync(() -> rInput, rContinuation);

			rLoopedStep.runAsync(fLoopIteration,
				consume(i -> loopAsync(i, rNextStep, rContinuation)),
				rContinuation);
		} else {
			rContinuation.suspend(this, rNextStep).resume(rInput);
		}
	}
}
