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
import consulo.util.concurrent.coroutine.Subroutine;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;

/**
 * A {@link Coroutine} step that executes another coroutine in the context of
 * the parent routine.
 *
 * @author eso
 */
public class CallSubroutine<I, O> extends CoroutineStep<I, O> {

	private final Coroutine<I, O> coroutine;

	/**
	 * Creates a new instance.
	 *
	 * @param coroutine The sub-coroutine
	 */
	public CallSubroutine(Coroutine<I, O> coroutine) {
		this.coroutine = coroutine;
	}

	/**
	 * Calls a coroutine as a subroutine of the coroutine this step is added
	 * to.
	 *
	 * @param coroutine The coroutine to invoke as a subroutine
	 * @return The new coroutine step
	 */
	public static <I, O> CallSubroutine<I, O> call(Coroutine<I, O> coroutine) {
		return new CallSubroutine<>(coroutine);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> previousExecution,
		CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
		// subroutine needs to be created on invocation because the return step
		// may change between invocations
		new Subroutine<>(coroutine, nextStep).runAsync(previousExecution,
			continuation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected O execute(I input, Continuation<?> continuation) {
		return new Subroutine<>(coroutine,
			apply(Function.identity())).runBlocking(input, continuation);
	}
}
