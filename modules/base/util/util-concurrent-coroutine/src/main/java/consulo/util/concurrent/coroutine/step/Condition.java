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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/********************************************************************
 * A {@link Coroutine} step that test a logical expression in the form of a
 * {@link Predicate} or {@link BiPredicate} and executes certain steps (which
 * may be a subroutines) based on the boolean result.
 *
 * @author eso
 */
public class Condition<I, O> extends CoroutineStep<I, O> {
	//~ Instance fields --------------------------------------------------------

	private final BiPredicate<? super I, Continuation<?>> pCondition;

	private final CoroutineStep<I, O> rRunIfTrue;

	private final CoroutineStep<I, O> rRunIfFalse;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param pCondition  The condition to test
	 * @param rRunIfTrue  The step to run if the condition is TRUE
	 * @param rRunIfFalse The step to run if the condition is FALSE
	 */
	public Condition(BiPredicate<? super I, Continuation<?>> pCondition,
		CoroutineStep<I, O> rRunIfTrue, CoroutineStep<I, O> rRunIfFalse) {
		Objects.requireNonNull(pCondition);
		Objects.requireNonNull(rRunIfTrue);

		this.pCondition = pCondition;
		this.rRunIfTrue = rRunIfTrue;
		this.rRunIfFalse = rRunIfFalse;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Tests a logical condition and executes a certain step if the condition is
	 * TRUE. To create a condition that also runs a step if the condition is
	 * FALSE either a subsequent call to {@link #orElse(CoroutineStep)} or the
	 * alternative method {@link #doIfElse(Predicate, CoroutineStep,
	 * CoroutineStep)}can be used. If no 'else' step is set the coroutine will
	 * terminate if the condition is not met.
	 *
	 * <p>This variant expects a unary predicate that only receives the input
	 * value. If the {@link Continuation} needs to be tested too the method
	 * {@link #doIf(BiPredicate, CoroutineStep)} can be used instead.</p>
	 *
	 * @param  fCondition The condition to test
	 * @param  rRunIfTrue The step to run if the condition is TRUE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIf(Predicate<? super I> fCondition,
		CoroutineStep<I, O> rRunIfTrue) {
		return new Condition<>((i, c) -> fCondition.test(i), rRunIfTrue, null);
	}

	/***************************************
	 * Tests a logical condition and executes a certain step if the condition is
	 * TRUE. To create a condition that also runs a step if the condition is
	 * FALSE either a subsequent call to {@link #orElse(CoroutineStep)} or the
	 * alternative method {@link #doIfElse(Predicate, CoroutineStep,
	 * CoroutineStep)}can be used. If no 'else' step is set the coroutine will
	 * terminate if the condition is not met.
	 *
	 * <p>This variant expects a binary predicate that also receives the current
	 * {@link Continuation}. If a test of the input value is sufficient the
	 * method {@link #doIf(Predicate, CoroutineStep)} can be used instead.</p>
	 *
	 * @param  fCondition The condition to test
	 * @param  rRunIfTrue The step to run if the condition is TRUE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIf(
		BiPredicate<? super I, Continuation<?>> fCondition,
		CoroutineStep<I, O> rRunIfTrue) {
		return new Condition<>(fCondition, rRunIfTrue, null);
	}

	/***************************************
	 * Tests a logical condition and executes certain steps if the condition is
	 * either TRUE or FALSE. A semantic alternative is the factory method {@link
	 * #doIf(BiPredicate, CoroutineStep)} in conjunction with the instance
	 * method {@link #orElse(CoroutineStep)}.
	 *
	 * <p>This variant expects a binary predicate that also receives the current
	 * {@link Continuation}. If a test of the input value is sufficient the
	 * method {@link #doIfElse(Predicate, CoroutineStep, CoroutineStep)} can be
	 * used instead.</p>
	 *
	 * @param  fCondition  The condition to test
	 * @param  rRunIfTrue  The step to run if the condition is TRUE
	 * @param  rRunIfFalse The step to run if the condition is FALSE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIfElse(
		BiPredicate<? super I, Continuation<?>> fCondition,
		CoroutineStep<I, O> rRunIfTrue, CoroutineStep<I, O> rRunIfFalse) {
		return new Condition<>(fCondition, rRunIfTrue, rRunIfFalse);
	}

	/***************************************
	 * Tests a logical condition and executes certain steps if the condition is
	 * either TRUE or FALSE. A semantic alternative is the factory method {@link
	 * #doIf(BiPredicate, CoroutineStep)} in conjunction with the instance
	 * method {@link #orElse(CoroutineStep)}.
	 *
	 * <p>This variant expects a unary predicate that only receives the input
	 * value. If the {@link Continuation} needs to be tested too the method
	 * {@link #doIf(BiPredicate, CoroutineStep)} can be used instead.</p>
	 *
	 * @param  fCondition  The condition to test
	 * @param  rRunIfTrue  The step to run if the condition is TRUE
	 * @param  rRunIfFalse The step to run if the condition is FALSE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIfElse(
		Predicate<? super I> fCondition, CoroutineStep<I, O> rRunIfTrue,
		CoroutineStep<I, O> rRunIfFalse) {
		return new Condition<>((i, c) -> fCondition.test(i), rRunIfTrue,
			rRunIfFalse);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a new instance with the condition and TRUE step of this and a
	 * certain step to execute if the condition is FALSE. This is just a
	 * semantic alternative to {@link #doIfElse(BiPredicate, CoroutineStep,
	 * CoroutineStep)}.
	 *
	 * @param  rRunIfFalse The step to run if the condition is FALSE
	 *
	 * @return A new conditional step
	 */
	public Condition<I, O> orElse(CoroutineStep<I, O> rRunIfFalse) {
		return new Condition<>(pCondition, rRunIfTrue, rRunIfFalse);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> previousExecution,
		CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution, i -> {
			CoroutineStep<I, O> rStep =
				pCondition.test(i, continuation) ? rRunIfTrue : rRunIfFalse;

			if (rStep != null) {
				// forward to rStep which handles future chaining and errors
				rStep.runAsync(previousExecution, nextStep, continuation);
			} else {
				terminateCoroutine(continuation);
			}
		});
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected O execute(I input, Continuation<?> continuation) {
		O rResult = null;

		if (pCondition.test(input, continuation)) {
			rResult = rRunIfTrue.runBlocking(input, continuation);
		} else if (rRunIfFalse != null) {
			rResult = rRunIfFalse.runBlocking(input, continuation);
		}

		return rResult;
	}
}
