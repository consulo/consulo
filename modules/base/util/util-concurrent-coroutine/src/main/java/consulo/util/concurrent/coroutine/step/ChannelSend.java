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

import consulo.util.concurrent.coroutine.ChannelId;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/********************************************************************
 * A coroutine step that sends a value into a channel. If the channel capacity
 * has been reached at the time of this step's invocation the coroutine
 * execution will be suspended until channel capacity becomes available. The
 * channel to send to will be queried (and if not existing created) by calling
 * {@link CoroutineScope#getChannel(ChannelId)}.
 *
 * <p>A send step returns the input value it sends so that it ca be processed
 * further in subsequent steps if needed.</p>
 *
 * @author eso
 */
public class ChannelSend<T> extends ChannelStep<T, T> {
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that sends to a channel the ID of which is
	 * provided in a state relation.
	 *
	 * @param fGetChannelId The function that will return the channel ID
	 */
	public ChannelSend(Function<Continuation<?>, ChannelId<T>> fGetChannelId) {
		super(fGetChannelId);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends until a value can be sent to a certain channel.
	 *
	 * @param  rId The ID of the channel to send to
	 *
	 * @return A new step instance
	 */
	public static <T> ChannelSend<T> send(ChannelId<T> rId) {
		return new ChannelSend<>(c -> rId);
	}

	/***************************************
	 * Suspends until a value can be sent to the channel with the ID provided by
	 * the given function.
	 *
	 * @param  fGetChannelId The function that will return the channel ID
	 *
	 * @return A new step instance
	 */
	public static <T> ChannelSend<T> send(
		Function<Continuation<?>, ChannelId<T>> fGetChannelId) {
		return new ChannelSend<>(fGetChannelId);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<T> previousExecution,
		CoroutineStep<T, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution,
			v -> getChannel(continuation).sendSuspending(
				continuation.suspend(this, nextStep).withValue(v)));
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected T execute(T input, Continuation<?> continuation) {
		getChannel(continuation).sendBlocking(input);

		return input;
	}
}
