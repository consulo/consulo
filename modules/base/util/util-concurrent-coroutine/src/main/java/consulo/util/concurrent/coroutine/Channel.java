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

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
public class Channel<T> implements AutoCloseable {

	private final ChannelId<T> id;

	private final BlockingQueue<T> channelData;

	private final Deque<Suspension<T>> sendQueue = new LinkedList<>();

	private final Deque<Suspension<T>> receiveQueue = new LinkedList<>();

	private final RunLock accessLock = new RunLock();

	private boolean closed = false;

	/**
	 * Creates a new instance.
	 *
	 * @param id       The channel identifier
	 * @param capacity The maximum number of values the channel can hold before
	 *                 blocking
	 */
	protected Channel(ChannelId<T> id, int capacity) {
		this.id = id;

		channelData = new LinkedBlockingQueue<>(capacity);
	}

	/**
	 * Throws a {@link ChannelClosedException} if this channel is already
	 * closed.
	 */
	public final void checkClosed() {
		if (isClosed()) {
			throw new ChannelClosedException(id);
		}
	}

	/**
	 * Closes this channel. All send and receive operations on a closed channel
	 * will throw a {@link ChannelClosedException}. If there are remaining
	 * suspensions in this channel they will also be failed with such an
	 * exception.
	 */
	@Override
	public void close() {
		accessLock.runLocked(() -> {
			closed = true;

			ChannelClosedException eClosed = new ChannelClosedException(id);

			for (Suspension<T> rSuspension : receiveQueue) {
				rSuspension.fail(eClosed);
			}
			for (Suspension<T> rSuspension : sendQueue) {
				rSuspension.fail(eClosed);
			}
		});
	}

	/**
	 * Returns the channel identifier.
	 *
	 * @return The channel ID
	 */
	public ChannelId<T> getId() {
		return id;
	}

	/**
	 * Returns the closed.
	 *
	 * @return The closed
	 */
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Receives a value from this channel, blocking if no data is available.
	 *
	 * @return The next value from this channel or NULL if the waiting for a
	 * value has been interrupted
	 */
	public T receiveBlocking() {
		return accessLock.supplyLocked(() -> {
			checkClosed();

			try {
				T rValue = channelData.take();

				resumeSenders();

				return rValue;
			} catch (InterruptedException e) {
				throw new CoroutineException(e);
			}
		});
	}

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
	public void receiveSuspending(Suspension<T> suspension) {
		accessLock.runLocked(() -> {
			checkClosed();

			T rValue = channelData.poll();

			if (rValue != null) {
				suspension.resume(rValue);
				resumeSenders();
			} else {
				receiveQueue.add(suspension);
			}
		});
	}

	/**
	 * Returns the number of values that can still be send to this channel. Due
	 * to the concurrent nature of channels this can only be a momentary value
	 * which needs to be interpreted with caution and necessary synchronization
	 * should be performed if applicable. Concurrently running coroutines could
	 * affect this value at any time.
	 *
	 * @return The remaining channel capacity
	 */
	public int remainingCapacity() {
		return channelData.remainingCapacity();
	}

	/**
	 * Sends a value into this channel, blocking if no capacity is available.
	 *
	 * @param value The value to send
	 */
	public void sendBlocking(T value) {
		accessLock.runLocked(() -> {
			checkClosed();

			try {
				channelData.put(value);
				resumeReceivers();
			} catch (InterruptedException e) {
				throw new CoroutineException(e);
			}
		});
	}

	/**
	 * Tries to send a value into this channel and resumes the execution of a
	 * {@link Coroutine} at the given step as soon as channel capacity becomes
	 * available. This can be immediately or, if the channel is full, only after
	 * some other code receives a values from this channel. Suspended senders
	 * will be served with a first-suspended-first-served policy.
	 *
	 * @param suspension rValue The value to send
	 */
	public void sendSuspending(Suspension<T> suspension) {
		accessLock.runLocked(() -> {
			checkClosed();

			if (channelData.offer(suspension.value())) {
				suspension.resume();
				resumeReceivers();
			} else {
				sendQueue.add(suspension);
			}
		});
	}

	/**
	 * Returns the current number of values in this channel. Due to the
	 * concurrent nature of channels this can only be a momentary value which
	 * needs to be interpreted with caution and necessary synchronization should
	 * be performed if applicable. Concurrently running coroutines could affect
	 * this value at any time.
	 *
	 * @return The current number of channel entries
	 */
	public int size() {
		return channelData.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("%s-%s", getClass().getSimpleName(), id);
	}

	/**
	 * Notifies suspended receivers that new data has become available in this
	 * channel.
	 */
	private void resumeReceivers() {
		while (channelData.size() > 0 && !receiveQueue.isEmpty()) {
			Suspension<T> rSuspension = receiveQueue.remove();

			rSuspension.ifNotCancelled(() -> {
				T rValue = channelData.remove();

				if (rValue != null) {
					rSuspension.resume(rValue);
				} else {
					receiveQueue.push(rSuspension);
				}
			});
		}
	}

	/**
	 * Notifies suspended senders that channel capacity has become available.
	 */
	private void resumeSenders() {
		while (channelData.remainingCapacity() > 0 && !sendQueue.isEmpty()) {
			Suspension<T> rSuspension = sendQueue.remove();

			rSuspension.ifNotCancelled(() -> {
				if (channelData.offer(rSuspension.value())) {
					rSuspension.resume();
				} else {
					sendQueue.push(rSuspension);
				}
			});
		}
	}
}
