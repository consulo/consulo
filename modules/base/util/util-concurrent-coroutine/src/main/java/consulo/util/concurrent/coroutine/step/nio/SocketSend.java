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
package consulo.util.concurrent.coroutine.step.nio;

import consulo.util.concurrent.coroutine.Continuation;

import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Implements asynchronous writing to a {@link AsynchronousSocketChannel}.
 *
 * @author eso
 */
public class SocketSend extends AsynchronousSocketStep {

	/**
	 * Creates a new instance.
	 *
	 * @param getSocketAddress A function that provides the target socket
	 *                         address from the current continuation
	 */
	public SocketSend(
		Function<Continuation<?>, SocketAddress> getSocketAddress) {
		super(getSocketAddress);
	}

	/**
	 * Suspends until all data from the input {@link ByteBuffer} has been sent
	 * to a network socket.The buffer must be initialized for sending, i.e. if
	 * necessary a call to {@link Buffer#flip()} must have been performed.
	 *
	 * <p>After the data has been fully sent {@link ByteBuffer#clear()} will be
	 * invoked on the buffer so that it can be used directly for subsequent
	 * writing to it. An example would be a following {@link SocketReceive} to
	 * implement a request-response scheme.</p>
	 *
	 * @param getSocketAddress A function that provides the target socket
	 *                         address from the current continuation
	 * @return A new step instance
	 */
	public static SocketSend sendTo(
		Function<Continuation<?>, SocketAddress> getSocketAddress) {
		return new SocketSend(getSocketAddress);
	}

	/**
	 * @see #sendTo(Function)
	 */
	public static SocketSend sendTo(SocketAddress socketAddress) {
		return sendTo(c -> socketAddress);
	}

	@Override
	protected boolean performAsyncOperation(int bytesProcessed,
		AsynchronousSocketChannel channel, ByteBuffer data,
		ChannelCallback<Integer, AsynchronousSocketChannel> callback) {
		if (data.hasRemaining()) {
			channel.write(data, data, callback);

			return false;
		} else {
			data.clear();

			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void performBlockingOperation(AsynchronousSocketChannel channel,
		ByteBuffer data) throws InterruptedException, ExecutionException {
		while (data.hasRemaining()) {
			channel.write(data).get();
		}

		data.clear();
	}
}
