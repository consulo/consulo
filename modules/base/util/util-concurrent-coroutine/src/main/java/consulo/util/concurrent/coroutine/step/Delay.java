//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License")
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

import consulo.util.concurrent.coroutine.*;
import consulo.util.lang.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A suspending {@link Coroutine} step that performs delayed executions.
 *
 * @author eso
 */
public class Delay<T> extends CoroutineStep<T, T> {

	private final Function<Continuation<?>, Pair<Long, TimeUnit>> getDuration;

	/**
	 * Creates a new instance.
	 *
	 * @param getDuration A function that determines the duration to sleep from
	 *                    the continuation
	 */
	public Delay(Function<Continuation<?>, Pair<Long, TimeUnit>> getDuration) {
		Objects.requireNonNull(getDuration);
		this.getDuration = getDuration;
	}

	/**
	 * Suspends the coroutine execution for a duration stored in a certain state
	 * relation. The lookup of the duration value follows the rules defined by
	 * {@link Continuation#getState(RelationType, Object)}.
	 *
	 * @param getDuration A function that determines the duration to sleep from
	 *                    the continuation
	 * @return A new step instance
	 */
	public static <T> Delay<T> sleep(
		Function<Continuation<?>, Pair<Long, TimeUnit>> getDuration) {
		return new Delay<>(getDuration);
	}

	/**
	 * Suspends the coroutine execution for a certain duration in milliseconds.
	 *
	 * @param milliseconds The milliseconds to sleep
	 * @see #sleep(long, TimeUnit)
	 */
	public static <T> Delay<T> sleep(long milliseconds) {
		return sleep(milliseconds, TimeUnit.MILLISECONDS);
	}

	/**
	 * Suspends the coroutine execution for a certain duration.
	 *
	 * @param duration The duration to sleep
	 * @param timeUnit The time unit of the duration
	 * @return A new step instance
	 */
	public static <T> Delay<T> sleep(long duration, TimeUnit timeUnit) {
		return new Delay<>(c -> Pair.create(duration, timeUnit));
	}

	@Override
	public T execute(T input, Continuation<?> continuation) {
		try {
			Pair<Long, TimeUnit> duration = getDuration.apply(continuation);

			duration.second.sleep(duration.first);
		} catch (Exception e) {
			throw new CoroutineException(e);
		}
		return input;
	}

	@Override
	public void runAsync(CompletableFuture<T> previousExecution,
		CoroutineStep<T, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution, v -> {

			Suspension<T> suspension = continuation.suspend(this, nextStep);

			Pair<Long, TimeUnit> duration = getDuration.apply(continuation);

			ScheduledFuture<?> delayedExecution = continuation.context()
				.getScheduler()
				.schedule(() -> suspension.resume(v), duration.first,
					duration.second);
			suspension.onCancel(Optional.of(() -> delayedExecution.cancel(true)));
		});
	}
}
