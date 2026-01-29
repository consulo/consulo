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
import consulo.util.concurrent.coroutine.Selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/********************************************************************
 * A coroutine step that suspends the coroutine execution until the first
 * matching result of several other asynchronously executed coroutines are
 * available. The selecting coroutine is then resumed with the selected result.
 * By default the result of the coroutine that finished first will be selected
 * but that can be modified by setting a different condition with {@link
 * #when(Predicate)}. If a result is selected all coroutines that are sill
 * running will be cancelled and execution of the selecting coroutine is
 * resumed.
 *
 * <p>The main purpose of this step is to non-blockingly select the first result
 * from other suspending coroutines although it can be used with arbitrary
 * coroutines and steps. It should be noted that if both suspending and
 * non-suspending selection targets are used the non-suspending targets will
 * almost always be selected. Even when using only suspending coroutines the
 * selection will be slightly biased towards the first coroutine because the
 * coroutines need to be launched sequentially, giving the first coroutine(s) a
 * head start.</p>
 *
 * <p>To select more than one result from the child coroutines the related step
 * implementation {@link Collect} can be used.</p>
 *
 * @author eso
 */
public class Select<I, O> extends CoroutineStep<I, O> {
	//~ Instance fields --------------------------------------------------------

	private final List<Coroutine<? super I, ? extends O>> aCoroutines =
		new ArrayList<>();

	private Predicate<Continuation<?>> pSelectCritiera = c -> true;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rFromCoroutines The coroutines to select from
	 */
	public Select(
		Collection<Coroutine<? super I, ? extends O>> rFromCoroutines) {
		if (rFromCoroutines.size() == 0) {
			throw new IllegalArgumentException(
				"At least one coroutine to select is required");
		}

		aCoroutines.addAll(rFromCoroutines);
	}

	/***************************************
	 * Copies the state of another instance.
	 *
	 * @param rOther The other instance
	 */
	private Select(Select<I, O> rOther) {
		aCoroutines.addAll(rOther.aCoroutines);

		pSelectCritiera = rOther.pSelectCritiera;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends the coroutine execution until one coroutine finishes and then
	 * resumes the execution with the result. By default the result of the first
	 * finished coroutine is selected. That can be modified by providing a
	 * different selection condition to {@link #when(Predicate)} which will
	 * return a new {@link Select} instance. Modified instances that select from
	 * additional coroutines or steps can be created with {@link #or(Coroutine)}
	 * and {@link #or(CoroutineStep)}.
	 *
	 * @param  rFromCoroutines The coroutines to select from
	 *
	 * @return A new step instance
	 */
	@SafeVarargs
	public static <I, O> Select<I, O> select(
		Coroutine<? super I, ? extends O>... rFromCoroutines) {
		return new Select<I, O>(asList(rFromCoroutines));
	}

	/***************************************
	 * Suspends the coroutine execution until one coroutine step finishes. The
	 * step arguments will be wrapped into new coroutines and then handed to
	 * {@link #select(Coroutine...)}.
	 *
	 * @param  rFromSteps The coroutine steps to select from
	 *
	 * @return A new step instance
	 */
	@SafeVarargs
	public static <I, O> Select<I, O> select(
		CoroutineStep<? super I, ? extends O>... rFromSteps) {
		return new Select<>(asList(rFromSteps).stream()
			.map(rStep -> new Coroutine<>(rStep))
			.collect(Collectors.toList()));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Creates a new instance that selects from an additional coroutine.
	 *
	 * @param  rCoroutine The additional coroutine to select from
	 *
	 * @return The new instance
	 */
	public Select<I, O> or(Coroutine<? super I, ? extends O> rCoroutine) {
		Select<I, O> aSelect = new Select<>(this);

		aSelect.aCoroutines.add(rCoroutine);

		return aSelect;
	}

	/***************************************
	 * Creates a new instance that selects from an additional step. The step
	 * will be wrapped into a new coroutine and handed to {@link
	 * #or(Coroutine)}.
	 *
	 * @param  rStep The additional step to select from
	 *
	 * @return The new instance
	 */
	public Select<I, O> or(CoroutineStep<? super I, ? extends O> rStep) {
		Select<I, O> aSelect = new Select<>(this);

		aSelect.aCoroutines.add(new Coroutine<>(rStep));

		return aSelect;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> previousExecution,
		CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution,
			rInput -> selectAsync(rInput, nextStep, continuation));
	}

	/***************************************
	 * Adds a condition for the result selection. If a succefully finished
	 * continuation matches the given predicate it will be selected as the step
	 * result.
	 *
	 * @param  pSelectCriteria A condition that checks if a result should be
	 *                         selected
	 *
	 * @return A new step instance
	 */
	public Select<I, O> when(Predicate<Continuation<?>> pSelectCriteria) {
		Select<I, O> aSelect = new Select<>(aCoroutines);

		aSelect.pSelectCritiera = pSelectCriteria;

		return aSelect;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected O execute(I input, Continuation<?> continuation) {
		// even if executed blocking the selection must happen asynchronously,
		// so we just run this step as a new coroutine in the current scope
		return new Coroutine<>(this).runAsync(continuation.scope(), input)
			.getResult();
	}

	/***************************************
	 * Performs the asynchronous selection.
	 *
	 * @param rInput        The input value
	 * @param rNextStep     The step to resume after the suspension
	 * @param rContinuation the current continuation
	 */
	void selectAsync(I rInput, CoroutineStep<O, ?> rNextStep,
		Continuation<?> rContinuation) {
		Selection<O, O, O> aSelection =
			Selection.ofSingleValue(this, rNextStep, rContinuation,
				pSelectCritiera);

		rContinuation.suspendTo(aSelection);

		aCoroutines.forEach(rCoroutine -> aSelection.add(
			rCoroutine.runAsync(rContinuation.scope(), rInput)));

		aSelection.seal();
	}
}
