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
 * A coroutine step that suspends the coroutine execution until the results of
 * several other asynchronously executed coroutines are available. The results
 * are then collected and handed over to the resuming step. By default the
 * results of all finished coroutines will be collected but that can be modified
 * by setting a different condition with {@link #when(Predicate)}. Also by
 * default all coroutines will be awaited before resuming but that can be
 * controlled with {@link #until(Predicate)}. If the collecting is finished all
 * coroutines that are still running will be cancelled.
 *
 * <p>To select exactly only one result from multiple coroutines the related
 * step implementation {@link Select} can be used.</p>
 *
 * @author eso
 */
public class Collect<I, O> extends CoroutineStep<I, Collection<O>> {
	//~ Instance fields --------------------------------------------------------

	private final List<Coroutine<? super I, ? extends O>> aCoroutines =
		new ArrayList<>();

	private Predicate<Continuation<?>> pCollectCritiera = c -> true;

	private Predicate<Continuation<?>> pCompletionCritiera = c -> false;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rFromCoroutines The coroutines to select from
	 */
	public Collect(
		Collection<Coroutine<? super I, ? extends O>> rFromCoroutines) {
		if (rFromCoroutines.size() == 0) {
			throw new IllegalArgumentException(
				"At least one coroutine to collect is required");
		}

		aCoroutines.addAll(rFromCoroutines);
	}

	/***************************************
	 * Copies the state of another instance.
	 *
	 * @param rOther The other instance
	 */
	private Collect(Collect<I, O> rOther) {
		aCoroutines.addAll(rOther.aCoroutines);

		pCollectCritiera = rOther.pCollectCritiera;
		pCompletionCritiera = rOther.pCompletionCritiera;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends the coroutine execution until all coroutines finish and then
	 * resumes the execution with a collection of the results. By default the
	 * result of the first finished coroutine is collected. That can be modified
	 * by providing a different selection condition to {@link #until(Predicate)}
	 * which will return a new {@link Collect} instance. Modified instances that
	 * select from additional coroutines or steps can be created with {@link
	 * #and(Coroutine)} and {@link #and(CoroutineStep)}.
	 *
	 * @param  rFromCoroutines The coroutines to select from
	 *
	 * @return A new step instance
	 */
	@SafeVarargs
	public static <I, O> Collect<I, O> collect(
		Coroutine<? super I, ? extends O>... rFromCoroutines) {
		return new Collect<I, O>(asList(rFromCoroutines));
	}

	/***************************************
	 * Suspends the coroutine execution until one coroutine step finishes. The
	 * step arguments will be wrapped into new coroutines and then handed to
	 * {@link #collect(Coroutine...)}.
	 *
	 * @param  rFromSteps The coroutine steps to select from
	 *
	 * @return A new step instance
	 */
	@SafeVarargs
	public static <I, O> Collect<I, O> collect(
		CoroutineStep<? super I, ? extends O>... rFromSteps) {
		return new Collect<>(asList(rFromSteps).stream()
			.map(rStep -> new Coroutine<>(rStep))
			.collect(Collectors.toList()));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Creates a new instance that collects the result of an additional
	 * coroutine.
	 *
	 * @param  rCoroutine The additional coroutine to select from
	 *
	 * @return The new instance
	 */
	public Collect<I, O> and(Coroutine<? super I, ? extends O> rCoroutine) {
		Collect<I, O> aCollect = new Collect<>(this);

		aCollect.aCoroutines.add(rCoroutine);

		return aCollect;
	}

	/***************************************
	 * Creates a new instance that collects the result of an additional step.
	 * The step will be wrapped into a new coroutine and handed to {@link
	 * #and(Coroutine)}.
	 *
	 * @param  rStep The additional step to select from
	 *
	 * @return The new instance
	 */
	public Collect<I, O> and(CoroutineStep<? super I, ? extends O> rStep) {
		Collect<I, O> aCollect = new Collect<>(this);

		aCollect.aCoroutines.add(new Coroutine<>(rStep));

		return aCollect;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> previousExecution,
		CoroutineStep<Collection<O>, ?> nextStep,
		Continuation<?> continuation) {
		continuation.continueAccept(previousExecution,
			rInput -> collectAsync(rInput, nextStep, continuation));
	}

	/***************************************
	 * Adds a condition for the termination of the result collection. If a
	 * succefully finished continuation matches the given predicate the
	 * collection will be completed and the suspension resumed with the result
	 * that has been collected so far.
	 *
	 * <p>Any collection criteria provided through {@link #when(Predicate)} are
	 * not automatically applied to the completion criteria and must therefore
	 * be handled explicitly in the completion test if necessary.</p>
	 *
	 * @param  pCompletionCriteria A condition that checks if a result should be
	 *                             selected
	 *
	 * @return A new step instance
	 */
	public Collect<I, O> until(Predicate<Continuation<?>> pCompletionCriteria) {
		Collect<I, O> aCollect = new Collect<>(aCoroutines);

		aCollect.pCompletionCritiera = pCompletionCriteria;

		return aCollect;
	}

	/***************************************
	 * Adds a condition for the result collection. If a succefully finished
	 * continuation matches the given predicate it will be collected into the
	 * step result.
	 *
	 * @param  pCollectCriteria A condition that checks if a result should be
	 *                          collected
	 *
	 * @return A new step instance
	 */
	public Collect<I, O> when(Predicate<Continuation<?>> pCollectCriteria) {
		Collect<I, O> aCollect = new Collect<>(aCoroutines);

		aCollect.pCollectCritiera = pCollectCriteria;

		return aCollect;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected Collection<O> execute(I input, Continuation<?> continuation) {
		// even if executed blocking the selection must happen asynchronously,
		// so we just run this step as a new coroutine in the current scope
		return new Coroutine<>(this).runAsync(continuation.scope(), input)
			.getResult();
	}

	/***************************************
	 * Performs the asynchronous collection.
	 *
	 * @param rInput        The input value
	 * @param rNextStep     The step to resume after the suspension
	 * @param rContinuation the current continuation
	 */
	void collectAsync(I rInput, CoroutineStep<Collection<O>, ?> rNextStep,
		Continuation<?> rContinuation) {
		Selection<Collection<O>, O, Collection<O>> aSelection =
			Selection.ofMultipleValues(this, rNextStep, rContinuation,
				pCompletionCritiera, pCollectCritiera);

		rContinuation.suspendTo(aSelection);

		aCoroutines.forEach(rCoroutine -> {
			aSelection.add(rCoroutine.runAsync(rContinuation.scope(), rInput));
		});

		aSelection.seal();
	}
}
