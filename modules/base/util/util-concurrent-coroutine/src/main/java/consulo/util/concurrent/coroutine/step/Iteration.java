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
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static consulo.util.concurrent.coroutine.step.CodeExecution.consume;

/********************************************************************
 * A step that implements suspendable iteration over an {@link Iterable} input
 * value. Each value returned by the iterator will be processed with a separate
 * execution of a certain coroutine step (which may be a subroutine).
 *
 * <p>The static factory methods {@link #forEach(CoroutineStep)}, {@link
 * #collectEach(CoroutineStep)}, and {@link #collectEachInto(Supplier,
 * CoroutineStep)} create instances that either discard or collect the results
 * of applying the iteration step.</p>
 *
 * @author eso
 */
public class Iteration<T, R, I extends Iterable<T>, C extends Collection<R>>
	extends CoroutineStep<I, C> {
	//~ Instance fields --------------------------------------------------------

	private final CoroutineStep<T, R> rProcessingStep;

	private final Supplier<C> fCollectionFactory;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param fCollectionFactory A supplier that returns a collection of the
	 *                           target type to store the processed values in or
	 *                           NULL for no collection
	 * @param rProcessingStep    The step to be applied to each value returned
	 *                           by the iterator
	 */
	public Iteration(Supplier<C> fCollectionFactory,
		CoroutineStep<T, R> rProcessingStep) {
		this.rProcessingStep = rProcessingStep;
		this.fCollectionFactory = fCollectionFactory;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Collects all processed elements into a list.
	 *
	 * @see #collectEachInto(Supplier, CoroutineStep)
	 */
	public static <T, R, I extends Iterable<T>> CoroutineStep<I, List<R>> collectEach(
		CoroutineStep<T, R> rProcessingStep) {
		return collectEachInto(() -> new ArrayList<>(), rProcessingStep);
	}

	/***************************************
	 * Iterates over the elements in an {@link Iterable} input value, processes
	 * each element with another coroutine step, and collects the resulting
	 * values into a target collection. If invoked asynchronously each iteration
	 * will be invoked as a separate suspension, but values are still processed
	 * sequentially. After the iteration has completed the coroutine continues
	 * with the next step with the collected values as it's input.
	 *
	 * @param  fCollectionFactory A supplier that returns a collection of the
	 *                            target type to store the processed values in
	 * @param  rProcessingStep    The step to process each value
	 *
	 * @return A new step instance
	 */
	public static <T, R, I extends Iterable<T>, C extends Collection<R>> CoroutineStep<I, C> collectEachInto(
		Supplier<C> fCollectionFactory, CoroutineStep<T, R> rProcessingStep) {
		return new Iteration<>(fCollectionFactory, rProcessingStep);
	}

	/***************************************
	 * Iterates over the elements in an {@link Iterable} input value and
	 * processes each element with another coroutine step. The processed values
	 * will be discarded, the returned step will always have a result of NULL.
	 * To collect the processed values {@link #collectEachInto(Supplier,
	 * CoroutineStep)} can be used instead.
	 *
	 * @param  rProcessingStep The step to process each value
	 *
	 * @return A new step instance
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, I extends Iterable<T>> CoroutineStep<I, Void> forEach(
		CoroutineStep<T, ?> rProcessingStep) {
		// needs to be raw as the actual return type of the processing step is
		// not known; but as the processing results are discarded the type of
		// the iteration step will be <I, ?> where the ? is forced to Void
		return new Iteration(null, rProcessingStep);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> previousExecution,
		CoroutineStep<C, ?> nextStep, Continuation<?> continuation) {
		C aResults =
			fCollectionFactory != null ? fCollectionFactory.get() : null;

		continuation.continueAccept(previousExecution,
			i -> iterateAsync(i.iterator(), aResults, nextStep, continuation));
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected C execute(I input, Continuation<?> continuation) {
		C aResults =
			fCollectionFactory != null ? fCollectionFactory.get() : null;

		for (T rValue : input) {
			R aResult = rProcessingStep.runBlocking(rValue, continuation);

			if (aResults != null) {
				aResults.add(aResult);
			}
		}

		return aResults;
	}

	/***************************************
	 * Performs the asynchronous iteration over all values in an iterator.
	 *
	 * @param rIterator     The iterator
	 * @param rResults      The collection to place the processed values in or
	 *                      NULL to collect no results
	 * @param rNextStep     The step to execute when the iteration is finished
	 * @param rContinuation The current continuation
	 */
	private void iterateAsync(Iterator<T> rIterator, C rResults,
		CoroutineStep<C, ?> rNextStep, Continuation<?> rContinuation) {
		if (rIterator.hasNext()) {
			CompletableFuture<T> fNextIteration =
				CompletableFuture.supplyAsync(() -> rIterator.next(),
					rContinuation);

			rProcessingStep.runAsync(fNextIteration, consume(o -> {
				if (rResults != null) {
					rResults.add(o);
				}

				iterateAsync(rIterator, rResults, rNextStep, rContinuation);
			}), rContinuation);
		} else {
			rContinuation.suspend(this, rNextStep).resume(rResults);
		}
	}
}
