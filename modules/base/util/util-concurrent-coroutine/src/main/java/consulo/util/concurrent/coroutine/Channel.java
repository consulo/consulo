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

import org.jspecify.annotations.Nullable;

/**
 * A channel that allows communication between {@link Coroutine Coroutines}. A
 * channel has a fixed capacity and suspends any further sending of data after
 * the capacity has been reached until capacity becomes available again when
 * data is requested by receivers. Receiving will be suspended if no more data
 * is available in a channel.
 *
 * <p>
 * Channels can be queried with the method
 * {@link CoroutineEnvironment#getChannel(ChannelId) getChannel(ChannelId)}
 * which is available in {@link CoroutineScope} and {@link CoroutineContext}. If
 * no channel exists at first access a new channel with a capacity of 1 will be
 * created. A channel with a different capacity can be created with
 * {@link CoroutineEnvironment#createChannel(ChannelId, int)
 * createChannel(ChannelId, int)}, but only if it doesn't exist already.
 * Channels can be removed with
 * {@link CoroutineEnvironment#removeChannel(ChannelId)
 * removeChannel(ChannelId)}.
 * </p>
 *
 * <p>
 * If a channel is needed for the communication between coroutines in different
 * scopes it needs to be created in a common context of the scopes. If it is
 * only needed in a particular scope it should be created there.
 * </p>
 *
 * <p>
 * Channels can be closed by invoking {@link #close()}. A closed channel rejects
 * any further send or receive calls by throwing a
 * {@link ChannelClosedException}. Upon a close all pending suspensions will
 * also be failed with that exception.
 * </p>
 *
 * @author eso
 */
public interface Channel<T> extends AutoCloseable {

    /**
     * Throws a {@link ChannelClosedException} if this channel is already
     * closed.
     */
    void checkClosed();

    /**
     * Closes this channel. All send and receive operations on a closed channel
     * will throw a {@link ChannelClosedException}. If there are remaining
     * suspensions in this channel they will also be failed with such an
     * exception.
     */
    @Override
    void close();

    /**
     * Returns the channel identifier.
     *
     * @return The channel ID
     */
    ChannelId<T> getId();

    /**
     * Returns the closed.
     *
     * @return The closed
     */
    boolean isClosed();

    /**
     * Receives a value from this channel, blocking if no data is available.
     *
     * @return The next value from this channel or NULL if the waiting for a
     * value has been interrupted
     */
    T receiveBlocking();

    /**
     * Tries to receive a value from this channel and resumes the execution of a
     * {@link Coroutine} at the given suspension as soon as a value becomes
     * available. This can be immediately or, if the channel is empty, only
     * after some other code sends a values into this channel. Suspended senders
     * will be served with a first-suspended-first-served policy.
     *
     * @param suspension The coroutine suspension to resume after data has been
     *                   receive
     */
    void receiveSuspending(Suspension<T> suspension);

    /**
     * Returns the number of values that can still be send to this channel. Due
     * to the concurrent nature of channels this can only be a momentary value
     * which needs to be interpreted with caution and necessary synchronization
     * should be performed if applicable. Concurrently running coroutines could
     * affect this value at any time.
     *
     * @return The remaining channel capacity
     */
    int remainingCapacity();

    /**
     * Sends a value into this channel, blocking if no capacity is available.
     *
     * @param value The value to send
     */
    void sendBlocking(@Nullable T value);

    /**
     * Tries to send a value into this channel and resumes the execution of a
     * {@link Coroutine} at the given step as soon as channel capacity becomes
     * available. This can be immediately or, if the channel is full, only after
     * some other code receives a values from this channel. Suspended senders
     * will be served with a first-suspended-first-served policy.
     *
     * @param suspension rValue The value to send
     */
    void sendSuspending(Suspension<T> suspension);

    /**
     * Returns the current number of values in this channel. Due to the
     * concurrent nature of channels this can only be a momentary value which
     * needs to be interpreted with caution and necessary synchronization should
     * be performed if applicable. Concurrently running coroutines could affect
     * this value at any time.
     *
     * @return The current number of channel entries
     */
    int size();
}
