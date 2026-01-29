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

import consulo.util.concurrent.coroutine.Channel;
import consulo.util.concurrent.coroutine.ChannelId;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineException;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.Objects;
import java.util.function.Function;


/********************************************************************
 * A base class for coroutine steps that perform {@link Channel} operations.
 *
 * @author eso
 */
public abstract class ChannelStep<I, O> extends CoroutineStep<I, O>
{
	//~ Instance fields --------------------------------------------------------

	private final Function<Continuation<?>, ChannelId<O>> fGetChannelId;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that operates on a channel the ID of which is
	 * provided by a certain function that will be applied to the continuation
	 * of an execution.
	 *
	 * @param fGetChannelId The function that will return the channel ID
	 */
	public ChannelStep(Function<Continuation<?>, ChannelId<O>> fGetChannelId)
	{
		Objects.requireNonNull(fGetChannelId);

		this.fGetChannelId = fGetChannelId;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the channel this step operates on.
	 *
	 * @param  rContinuation The current continuation
	 *
	 * @return The channel
	 */
	public Channel<O> getChannel(Continuation<?> rContinuation)
	{
		ChannelId<O> rId = fGetChannelId.apply(rContinuation);

		if (rId == null)
		{
			throw new CoroutineException(
				"No channel ID returned by %s",
				fGetChannelId);
		}

		return rContinuation.getChannel(rId);
	}
}
