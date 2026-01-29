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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link Suspension} subclass that selects the suspension result from one or
 * more of multiple continuations based on certain criteria. If a child
 * continuation fails the selection step will also fail. If a child continuation
 * is cancelled, it will simply be ignored.
 *
 * @author eso
 */
public class Selection<T, V, R> extends Suspension<T> {

	private final CoroutineStep<R, ?> resumeSelectionStep;

	private final Predicate<Continuation<?>> checkSelect;

	private final Predicate<Continuation<?>> checkComplete;

	private final boolean singleValue;

	private final List<V> results = new ArrayList<>();

	private final List<Continuation<? extends V>> continuations = new ArrayList<>();

	private final RunLock stateLock = new RunLock();

	private boolean sealed = false;

	private boolean finished = false;

	private Runnable finishAction = this::resume;

	/**
	 * Internal constructor to create a new instance. Public creation through
	 * {@link #ofSingleValue(CoroutineStep, CoroutineStep, Continuation,
	 * Predicate)} or
	 * {@link #ofMultipleValues(CoroutineStep, CoroutineStep, Continuation,
	 * Predicate, Predicate)} for correct generic typing.
	 *
	 * @param suspendingStep The step that initiated the suspension
	 * @param resumeStep     The step to resume the execution with
	 * @param continuation   The continuation of the execution
	 * @param checkComplete  The condition for the termination of the selection
	 * @param checkSelect    The condition for the selection of results
	 * @param singleValue    TRUE for the selection of a single value, FALSE for
	 *                       a collection of values
	 */
	private Selection(CoroutineStep<?, T> suspendingStep,
			CoroutineStep<R, ?> resumeStep, Continuation<?> continuation,
			Predicate<Continuation<?>> checkComplete,
			Predicate<Continuation<?>> checkSelect, boolean singleValue) {
		super(suspendingStep, null, continuation);

		this.resumeSelectionStep = resumeStep;
		this.checkSelect = checkSelect;
		this.checkComplete = checkComplete;
		this.singleValue = singleValue;
	}

	/**
	 * Creates a new instance for the selection of multiple values. If no values
	 * are selected the result will be an empty collection.
	 *
	 * @param suspendingStep The step that initiated the suspension
	 * @param resumeStep     The step to resume the execution with
	 * @param rContinuation  The continuation of the execution
	 * @param checkComplete  The condition for the termination of the selection
	 * @param checkSelect    The condition for the selection of results
	 * @return The new instance
	 */
	public static <T, V> Selection<T, V, Collection<V>> ofMultipleValues(
			CoroutineStep<?, T> suspendingStep,
			CoroutineStep<Collection<V>, ?> resumeStep,
			Continuation<?> rContinuation, Predicate<Continuation<?>> checkComplete,
			Predicate<Continuation<?>> checkSelect) {
		Selection<T, V, Collection<V>> aSelection = new Selection<>(suspendingStep, resumeStep, rContinuation,
				checkComplete, checkSelect, false);

		return aSelection;
	}

	/**
	 * Creates a new instance for the selection of a single value. If no value
	 * is selected the result will be NULL.
	 *
	 * @param suspendingStep The step that initiated the suspension
	 * @param resumeStep     The step to resume the execution with
	 * @param continuation   The continuation of the execution
	 * @param checkSelect    The condition for the selection of results
	 * @return The new instance
	 */
	public static <T> Selection<T, T, T> ofSingleValue(
			CoroutineStep<?, T> suspendingStep, CoroutineStep<T, ?> resumeStep,
			Continuation<?> continuation, Predicate<Continuation<?>> checkSelect) {
		return new Selection<>(suspendingStep, resumeStep, continuation,
				c -> true, checkSelect, true);
	}

	/**
	 * Adds a continuation to this group.
	 *
	 * @param continuation The continuation
	 */
	public void add(Continuation<? extends V> continuation) {
		stateLock.runLocked(() -> {
			if (sealed) {
				throw new IllegalStateException("Selection is sealed");
			}
			// first add to make sure remove after an immediate return by
			// the following callbacks is applied
			continuations.add(continuation);
		});

		continuation.onFinish(this::continuationFinished)
				.onCancel(this::continuationCancelled)
				.onError(this::continuationFailed);

		if (finished) {
			continuation.cancel();
		}
	}

	@Override
	public void cancel() {
		stateLock.runLocked(() -> {
			finished = true;
			finishAction = this::cancel;

			checkComplete();
		});
	}

	/**
	 * Seals this instance so that no more coroutines can be added with
	 * {@link #add(Continuation)}. Sealing is necessary to allow the adding of
	 * further coroutines even if previously added coroutines have already
	 * finished execution.
	 */
	public void seal() {
		stateLock.runLocked(() -> {
			sealed = true;
			checkComplete();
		});
	}

	@Override
	public String toString() {
		return String.format("%s[%s -> %s]", getClass().getSimpleName(),
				suspendingStep(), resumeSelectionStep);
	}

	/**
	 * Notified when a continuation is cancelled. Cancelled continuations will
	 * only be removed but the selection will continue.
	 *
	 * @param continuation The finished continuation
	 */
	void continuationCancelled(Continuation<? extends V> continuation) {
		stateLock.runLocked(() -> {
			continuations.remove(continuation);
			checkComplete();
		});
	}

	/**
	 * Notified when the execution of a continuation failed. In that case the
	 * full selection will fail too.
	 *
	 * @param continuation The finished continuation
	 */
	void continuationFailed(Continuation<? extends V> continuation) {
		stateLock.runLocked(() -> {
			continuations.remove(continuation);
			finished = true;
			finishAction = () -> fail(continuation.getError());

			checkComplete();
		});
	}

	/**
	 * Notified when a continuation is finished.
	 *
	 * @param continuation The finished continuation
	 */
	void continuationFinished(Continuation<? extends V> continuation) {
		stateLock.runLocked(() -> {
			continuations.remove(continuation);

			if (!finished) {
				if (checkSelect.test(continuation)) {
					results.add(continuation.getResult());
				}
				finished = checkComplete.test(continuation);
			}
			checkComplete();
		});
	}

	/**
	 * Overridden to resume the selection result step instead.
	 *
	 * @see Continuation#resumeAsync(CoroutineStep, Object)
	 */
	@Override
	void resumeAsync() {
		// type safety is ensured by the factory methods
		@SuppressWarnings("unchecked")
		R result = (R) (singleValue ? (results.size() >= 1 ? results.get(0) : null) : results);

		continuation().resumeAsync(resumeSelectionStep, result);
	}

	/**
	 * Resumes this selection if it is sealed and contains no more
	 * continuations.
	 */
	private void checkComplete() {
		if (finished && !continuations.isEmpty()) {
			// cancel all remaining continuations if already finished; needs to
			// be done with a copied list because cancel may modify the list
			new ArrayList<>(continuations).forEach(Continuation::cancel);
		}
		// only finish if all child continuations have finished to race
		// conditions with subsequent step executions
		if (sealed && continuations.isEmpty()) {
			finished = true;

			// will either resume, cancel, or fail this suspension
			finishAction.run();
		}
	}
}
