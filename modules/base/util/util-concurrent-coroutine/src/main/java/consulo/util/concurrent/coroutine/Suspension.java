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

import java.util.Optional;

/**
 * Encapsulates the data that represents a suspended {@link Coroutine}. The
 * execution can be resumed by invoking {@link #resume()}.
 *
 * @author eso
 */
public class Suspension<T> {
	private final CoroutineStep<?, T> suspendingStep;

	private final CoroutineStep<T, ?> resumeStep;

	private final Continuation<?> continuation;

	private final RunLock cancelLock = new RunLock();

	private T value;

	private boolean cancelled = false;

	private Optional<Runnable> cancelHandler = Optional.empty();

	/**
	 * Creates a new instance. The input value for the resume step is not
	 * provided here because it is typically not available upon suspension
	 * because it will only become available when the suspension is resumed
	 * (e.g. when receiving data). To resume execution with an explicit input
	 * value the method {@link #resume(Object)} can be used. If the resume
	 * should occur at a different time than the availability of the input value
	 * a suspension can be updated by calling {@link #withValue(Object)}. In
	 * that case {@link #resume()} can be used later to resume the execution.
	 *
	 * @param suspendingStep The step that initiated the suspension
	 * @param resumeStep     The step to resume the execution with
	 * @param continuation   The continuation of the execution
	 */
	protected Suspension(CoroutineStep<?, T> suspendingStep,
		CoroutineStep<T, ?> resumeStep, Continuation<?> continuation) {
		this.suspendingStep = suspendingStep;
		this.resumeStep = resumeStep;
		this.continuation = continuation;
	}

	/**
	 * Cancels this suspension. This will {@link Continuation#cancel() cancel}
	 * the continuation. Tries to resume a cancelled suspension will be ignored.
	 * If a cancel handler has been registered with {@link #onCancel(Option)} it
	 * will be invoked.
	 */
	public void cancel() {
		cancelLock.runLocked(() -> {
			if (!cancelled) {
				cancelHandler.ifPresent(Runnable::run);
			}
			cancelled = true;
		});

		if (!continuation.isFinished()) {
			continuation.cancel();
		}
	}

	/**
	 * Returns the continuation of the suspended coroutine.
	 *
	 * @return The continuation
	 */
	public final Continuation<?> continuation() { // NOSONAR
		return continuation;
	}

	/**
	 * Cancels this suspension because of an error. This will
	 * {@link Continuation#fail(Throwable) fail} the continuation. Tries to
	 * resume a failed suspension will be ignored.
	 *
	 * @param error The error exception
	 */
	public void fail(Throwable error) {
		cancelLock.runLocked(() -> cancelled = true);

		if (!continuation.isCancelled()) {
			continuation.fail(error);
		}
	}

	/**
	 * Executes code only if this suspension has not (yet) been cancel. The
	 * given code will be executed with a lock on the cancelation state to
	 * prevent race conditions if other threads try to cancel a suspension while
	 * it is resumed.
	 *
	 * @param code The code to execute only if this suspension is not cancelled
	 */
	public void ifNotCancelled(Runnable code) {
		cancelLock.runLocked(() -> {
			if (!cancelled) {
				code.run();
			}
		});
	}

	/**
	 * Checks if the this suspension has been cancelled.
	 *
	 * @return TRUE if the suspension has been cancelled
	 */
	public final boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Sets an optional handler to be invoked if this suspension is cancelled.
	 * Only one handler at a time is supported, setting a new one will replace
	 * any previously registered handler.
	 *
	 * @param cancelHandler The optional cancel handler
	 */
	public void onCancel(Optional<Runnable> cancelHandler) {
		this.cancelHandler = cancelHandler;

	}

	/**
	 * Resumes the execution of the suspended coroutine with the input value
	 * provided to the constructor.
	 *
	 * @see #resume(Object)
	 */
	public final void resume() {
		resume(value);
	}

	/**
	 * Resumes the execution of the suspended coroutine with the given value.
	 *
	 * @param value The input value to the resumed step
	 */
	public void resume(T value) {
		if (!cancelled) {
			this.value = value;
			continuation.suspensionResumed(this);

			resumeAsync();
		}
	}

	/**
	 * Returns the suspending step.
	 *
	 * @return The suspending step
	 */
	public final CoroutineStep<?, T> suspendingStep() {
		return suspendingStep;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("%s[%s -> %s]", getClass().getSimpleName(),
			suspendingStep, resumeStep);
	}

	/**
	 * Returns the value of this suspension. The value will be used as the input
	 * of the resumed step.
	 *
	 * @return The suspension value
	 */
	public final T value() {
		return value;
	}

	/**
	 * Sets the suspension value and returns this instance so that it can be
	 * used as an updated argument to method calls.
	 *
	 * @param value The new value
	 * @return This instance
	 */
	public Suspension<T> withValue(T value) {
		this.value = value;

		return this;
	}

	/**
	 * Resumes this suspension by asynchronously executing the resume step.
	 *
	 * @see Continuation#resumeAsync(CoroutineStep, Object)
	 */
	void resumeAsync() {
		continuation.resumeAsync(resumeStep, value);
	}
}
