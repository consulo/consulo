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
 * A coroutine step that receives a value from a channel. If the channel is
 * empty at the time of this step's invocation the coroutine execution will be
 * suspended until channel data becomes available. The channel to receive from
 * will be queried (and if not existing created) with {@link
 * CoroutineScope#getChannel(ChannelId)}.
 *
 * @author eso
 */
public class ChannelReceive<T> extends ChannelStep<Void, T> {
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that receives from a channel the ID of which is
	 * provided in a state relation.
	 *
	 * @param fGetChannelId The function that will return the channel ID
	 */
	public ChannelReceive(
		Function<Continuation<?>, ChannelId<T>> fGetChannelId) {
		super(fGetChannelId);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends until a value can be received from a certain channel.
	 *
	 * @param  rId The ID of the channel to receive from
	 *
	 * @return A new instance of this class
	 */
	public static <T> ChannelReceive<T> receive(ChannelId<T> rId) {
		return new ChannelReceive<>(c -> rId);
	}

	/***************************************
	 * Suspends until a value can be received from the channel with the ID
	 * provided by the given function.
	 *
	 * @param  fGetChannelId The function that will return the channel ID
	 *
	 * @return A new step instance
	 */
	public static <T> ChannelReceive<T> receive(
		Function<Continuation<?>, ChannelId<T>> fGetChannelId) {
		return new ChannelReceive<>(fGetChannelId);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<Void> previousExecution,
		CoroutineStep<T, ?> nextStep, Continuation<?> continuation) {
		continuation.continueAccept(previousExecution,
			nothing -> getChannel(continuation).receiveSuspending(
				continuation.suspend(this, nextStep)));
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected T execute(Void input, Continuation<?> continuation) {
		return getChannel(continuation).receiveBlocking();
	}
}
