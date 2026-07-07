package consulo.util.concurrent.coroutine.internal;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineStep;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A coroutine subclass for the invocation of coroutines as subroutines in the
 * context of another execution.
 *
 * @author eso
 */
public class Subroutine<I, T, O> extends CoroutineImpl<I, O> {

	/**
	 * Creates a new instance that invokes the code of another coroutine as a
	 * subroutine and then returns the control flow to a step in the invoking
	 * subroutine. The code of the original coroutine will be copied into this
	 * instance, not referenced directly.
	 *
	 * @param coroutine  The coroutine to invoke as a subroutine
	 * @param returnStep The step to return to after the subroutine execution
	 */
	public Subroutine(Coroutine<I, T> coroutine, @Nullable CoroutineStep<T, O> returnStep) {
		init(((CoroutineImpl<I, T>) coroutine).getRequiredCode().withLastStep(new SubroutineReturn<>(Objects.requireNonNull(returnStep))), null);
	}

	/**
	 * Executes this subroutine asynchronously in the given future and
	 * continuation.
	 *
	 * @param execution    The execution future
	 * @param continuation The continuation of the execution
	 */
	public void runAsync(CompletableFuture<I> execution,
		Continuation<?> continuation) {
		((ContinuationImpl<?>) continuation).subroutineStarted(this);

		getRequiredCode().runAsync(execution, null, continuation);
	}

	/**
	 * Executes this subroutine synchronously in the given continuation.
	 *
	 * @param input        The input value
	 * @param continuation The continuation of the execution
	 * @return The result of the execution
	 */
	public O runBlocking(I input, Continuation<?> continuation) {
		((ContinuationImpl<?>) continuation).subroutineStarted(this);

		return getRequiredCode().runBlocking(input, continuation);
	}

	/**
	 * The final step of a coroutine execution inside another coroutine.
	 *
	 * @author eso
	 */
	static class SubroutineReturn<I, O> extends CoroutineStep<I, O> {

		private final CoroutineStep<I, O> returnStep;

		/**
		 * Sets the return step.
		 *
		 * @param returnStep The new return step
		 */
		public SubroutineReturn(CoroutineStep<I, O> returnStep) {
			this.returnStep = returnStep;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void runAsync(
			CompletableFuture<I> previousExecution,
			@Nullable CoroutineStep<O, ?> nextStep,
			Continuation<?> continuation
		) {
			((ContinuationImpl<?>) continuation).subroutineFinished();

			returnStep.runAsync(previousExecution, nextStep, continuation);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("NullAway")
		protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
			((ContinuationImpl<?>) continuation).subroutineFinished();

			return returnStep.runBlocking(input, continuation);
		}
	}
}
