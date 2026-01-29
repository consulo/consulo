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
package consulo.util.concurrent.coroutine;

import consulo.util.concurrent.coroutine.internal.RunLock;
import consulo.util.dataholder.UserDataHolderBase;

import java.util.HashMap;
import java.util.Map;

/**
 * A base class for environments to execute coroutines in. The main common
 * property of environments is that they manage the {@link Channel channels} for
 * the communication between coroutines. They allow to set relations for
 * coroutine configuration and/or state.
 *
 * @author eso
 */
public abstract class CoroutineEnvironment extends UserDataHolderBase {

	private final Map<ChannelId<?>, Channel<?>> channels = new HashMap<>();

	private final RunLock channelLock = new RunLock();

	/**
	 * Creates a new channel with a certain capacity and stores it in this
	 * context for lookup with {@link #getChannel(ChannelId)}. If a channel with
	 * the given ID already exists an exception will be thrown to prevent
	 * accidental overwriting of channels.
	 *
	 * @param id       The channel ID
	 * @param capacity The channel capacity
	 * @return The new channel
	 * @throws IllegalArgumentException If a channel with the given ID already
	 *                                  exists
	 */
	public <T> Channel<T> createChannel(ChannelId<T> id, int capacity) {
		return channelLock.supplyLocked(() -> {
			if (channels.containsKey(id)) {
				throw new IllegalArgumentException(
					String.format("Channel %s already exists", id));
			}
			Channel<T> aChannel = new Channel<>(id, capacity);

			channels.put(id, aChannel);

			return aChannel;
		});
	}

	/**
	 * Returns a channel for a certain ID. If no such channel exists a new
	 * channel with a capacity of 1 (one) entry will be created and stored under
	 * the given ID. To prevent this the channel needs to be created in advance
	 * by calling {@link #createChannel(ChannelId, int)}.
	 *
	 * @param id The channel ID
	 * @return The channel for the given ID
	 */
	@SuppressWarnings("unchecked")
	public <T> Channel<T> getChannel(ChannelId<T> id) {
		return channelLock.supplyLocked(() -> {
			Channel<T> rChannel = (Channel<T>) channels.get(id);

			if (rChannel == null) {
				rChannel = createChannel(id, 1);
			}
			return rChannel;
		});
	}

	/**
	 * Checks whether this context contains a certain channel. As with the other
	 * channel access methods the result depends on the current execution state
	 * of the coroutines in this context. It is recommended to only invoke this
	 * method if concurrent modification of the queries channel will not occur.
	 *
	 * @param id The channel ID
	 * @return TRUE if the channel exists in this context
	 */
	public boolean hasChannel(ChannelId<?> id) {
		return channelLock.supplyLocked(() -> channels.containsKey(id));
	}

	/**
	 * Removes a channel from this context. This method should be applied with
	 * caution because concurrently running coroutines may try to access or
	 * event re-create the channel in parallel. It is recommended to invoke this
	 * method only on contexts without running coroutines that access the
	 * channel ID. Otherwise synchronization is necessary.
	 *
	 * @param id The channel ID
	 */
	public void removeChannel(ChannelId<?> id) {
		channelLock.runLocked(() -> channels.remove(id));
	}
}
