//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import consulo.util.concurrent.coroutine.internal.EmptyCoroutine;
import consulo.util.concurrent.coroutine.internal.Subroutine;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;

/**
 * A {@link Coroutine} step that executes another coroutine in the context of
 * the parent routine. The sub-coroutine can be provided directly, or lazily via
 * a {@link Supplier} that is evaluated only when the step is executed (after the
 * previous step has completed). The lazy form allows using a sub-coroutine that
 * is bound to a value produced earlier in the same chain.
 *
 * @author eso
 */
public class CallSubroutine<I, O> extends CoroutineStep<I, O> {

	private final Function<Continuation<?>, ? extends Coroutine<I, O>> coroutineFactory;

	private CallSubroutine(Function<Continuation<?>, ? extends Coroutine<I, O>> coroutineFactory) {
		this.coroutineFactory = coroutineFactory;
	}

	/**
	 * Calls a coroutine as a subroutine of the coroutine this step is added
	 * to.
	 *
	 * @param coroutine The coroutine to invoke as a subroutine
	 * @return The new coroutine step
	 */
	public static <I, O> CallSubroutine<I, O> call(Coroutine<I, O> coroutine) {
		return new CallSubroutine<>(continuation -> coroutine);
	}

	/**
	 * Calls a coroutine, produced lazily by the given supplier, as a subroutine
	 * of the coroutine this step is added to. The supplier is evaluated when the
	 * step runs, so the produced coroutine may depend on values computed by
	 * earlier steps of the parent chain.
	 *
	 * @param coroutineSupplier The supplier of the coroutine to invoke as a subroutine
	 * @return The new coroutine step
	 */
	public static <I, O> CallSubroutine<I, O> call(Supplier<? extends Coroutine<I, O>> coroutineSupplier) {
		return new CallSubroutine<>(continuation -> coroutineSupplier.get());
	}

	/**
	 * Calls a coroutine, produced lazily from the current {@link Continuation}, as a
	 * subroutine of the coroutine this step is added to. The factory is evaluated when
	 * the step runs, so the produced coroutine may be bound to data stored in the
	 * continuation's user data by earlier steps of the parent chain.
	 *
	 * @param coroutineFactory The factory producing the coroutine to invoke as a subroutine
	 * @return The new coroutine step
	 */
	public static <I, O> CallSubroutine<I, O> call(Function<Continuation<?>, ? extends Coroutine<I, O>> coroutineFactory) {
		return new CallSubroutine<>(coroutineFactory);
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
		// evaluate the factory only after the previous step has completed, so the produced
		// coroutine may be bound to values (or continuation user data) computed earlier in the chain
		previousExecution.whenComplete((input, error) -> {
			if (error != null) {
				continuation.continueApply(previousExecution, in -> null, nextStep);
				return;
			}
			runSubroutine(coroutineFactory.apply(continuation), previousExecution, nextStep, continuation);
		});
	}

	@SuppressWarnings("NullAway")
	private void runSubroutine(
		@Nullable Coroutine<I, O> subCoroutine,
		CompletableFuture<I> previousExecution,
		@Nullable CoroutineStep<O, ?> nextStep,
		Continuation<?> continuation
	) {
		// an empty coroutine performs no work and cannot be turned into a subroutine, so just continue
		if (subCoroutine instanceof EmptyCoroutine) {
			continuation.continueApply(previousExecution, input -> null, nextStep);
			return;
		}

		// subroutine needs to be created on invocation because the return step
		// may change between invocations
		new Subroutine<>(subCoroutine, nextStep).runAsync(previousExecution, continuation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("NullAway")
	protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
		// NullAway problem: input and output are nullable by method contract but in actual usage input can be null only if I is nullable.
		// We cannot explain this to the static validator, so suppressing NullAway validation.
		Coroutine<I, O> subCoroutine = coroutineFactory.apply(continuation);
		if (subCoroutine instanceof EmptyCoroutine) {
			return null;
		}
		return new Subroutine<>(subCoroutine, apply(Function.identity())).runBlocking(input, continuation);
	}
}
