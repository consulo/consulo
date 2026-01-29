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
package consulo.util.concurrent.coroutine.step.nio;

import consulo.util.concurrent.coroutine.*;
import consulo.util.concurrent.coroutine.internal.AutoClosableRegister;
import consulo.util.dataholder.Key;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * The base class for coroutine steps that perform communication on instances of
 * {@link AsynchronousSocketChannel}. The channel will be opened and connected
 * as necessary. It may also be provided before the step is invoked in a state
 * relation with the type {@link #SOCKET_CHANNEL}. If the channel is opened by
 * this step it will have the {@link MetaTypes#MANAGED} flag so that it will be
 * automatically closed when the {@link CoroutineScope} finishes.
 *
 * @author eso
 */
public abstract class AsynchronousSocketStep
    extends AsynchronousChannelStep<ByteBuffer, ByteBuffer> {

    /**
     * State: the {@link AsynchronousSocketChannel} that the steps in a
     * coroutine operate on.
     */
    public static final Key<AsynchronousSocketChannel> SOCKET_CHANNEL = Key.create(AsynchronousSocketChannel.class);

    private final Function<Continuation<?>, SocketAddress> getSocketAddress;

    /**
     * Creates a new instance that connects to the socket with the address
     * provided by the given factory. The factory may return NULL if the step
     * should connect to a channel that is stored in a state relation with the
     * type {@link #SOCKET_CHANNEL}.
     *
     * @param getSocketAddress A function that provides the socket address to
     *                         connect to from the current continuation
     */
    public AsynchronousSocketStep(
        Function<Continuation<?>, SocketAddress> getSocketAddress) {
        Objects.requireNonNull(getSocketAddress);

        this.getSocketAddress = getSocketAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAsync(CompletableFuture<ByteBuffer> previousExecution,
                         CoroutineStep<ByteBuffer, ?> nextStep, Continuation<?> continuation) {
        continuation.continueAccept(previousExecution,
            b -> connectAsync(b, continuation.suspend(this, nextStep)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ByteBuffer execute(ByteBuffer input,
                                 Continuation<?> continuation) {
        try {
            AsynchronousSocketChannel channel = getSocketChannel(continuation);

            if (channel.getRemoteAddress() == null) {
                channel.connect(getSocketAddress(continuation)).get();
            }

            performBlockingOperation(channel, input);
        }
        catch (Exception e) {
            throw new CoroutineException(e);
        }

        return input;
    }

    /**
     * Returns the address of the socket to connect to.
     *
     * @param continuation The current continuation
     * @return The socket address
     */
    protected SocketAddress getSocketAddress(Continuation<?> continuation) {
        return getSocketAddress.apply(continuation);
    }

    /**
     * Returns the socket address factory of this step.
     *
     * @return The socket address factory function
     */
    protected Function<Continuation<?>, SocketAddress> getSocketAddressFactory() {
        return getSocketAddress;
    }

    /**
     * Returns the channel to be used by this step. This first checks the
     * currently exexcuting coroutine in the continuation parameter for an
     * existing {@link #SOCKET_CHANNEL} relation. If that doesn't exists or the
     * channel is closed a new {@link AsynchronousSocketChannel} will be opened
     * and stored in the coroutine relation. Using the coroutine to store the
     * channel allows coroutines to be structured so that multiple subroutines
     * perform communication on different channels.
     *
     * @param continuation The continuation to query for an existing channel
     * @return The socket channel
     * @throws IOException If opening the channel fails
     */
    protected AsynchronousSocketChannel getSocketChannel(
        Continuation<?> continuation) throws IOException {
        Coroutine<?, ?> rCoroutine = continuation.getCurrentCoroutine();

        AsynchronousSocketChannel rChannel = rCoroutine.getUserData(SOCKET_CHANNEL);

        if (rChannel == null || !rChannel.isOpen()) {
            rChannel = AsynchronousSocketChannel.open(getChannelGroup(continuation));
            rCoroutine.putUserData(SOCKET_CHANNEL, rChannel);

            AutoClosableRegister.register(rCoroutine, rChannel);
        }

        return rChannel;
    }

    /**
     * Implementation of the ChannelOperation functional interface method
     * signature.
     *
     * @see AsynchronousChannelStep.ChannelOperation#execute(int,
     * java.nio.channels.AsynchronousChannel, ByteBuffer,
     * AsynchronousChannelStep.ChannelCallback)
     */
    protected abstract boolean performAsyncOperation(int bytesProcessed,
                                                     AsynchronousSocketChannel channel, ByteBuffer data,
                                                     ChannelCallback<Integer, AsynchronousSocketChannel> callback)
        throws Exception;

    /**
     * Must be implemented for the blocking execution of a step. It receives an
     * {@link AsynchronousSocketChannel} which must be accessed through the
     * blocking API (like {@link Future#get()}).
     *
     * @param channel The channel to perform the operation on
     * @param data    The byte buffer for the operation data
     * @throws Exception Any kind of exception may be thrown
     */
    protected abstract void performBlockingOperation(
        AsynchronousSocketChannel channel, ByteBuffer data) throws Exception;

    /**
     * Opens and connects a {@link AsynchronousSocketChannel} to the
     * {@link SocketAddress} of this step and then performs the channel
     * operation asynchronously.
     *
     * @param data       The byte buffer of the data to be processed
     * @param suspension The coroutine suspension to be resumed when the
     *                   operation is complete
     */
    private void connectAsync(ByteBuffer data,
                              Suspension<ByteBuffer> suspension) {
        try {
            AsynchronousSocketChannel rChannel =
                getSocketChannel(suspension.continuation());

            if (rChannel.getRemoteAddress() == null) {
                SocketAddress rSocketAddress =
                    getSocketAddress.apply(suspension.continuation());

                rChannel.connect(rSocketAddress, data,
                    new ChannelCallback<>(rChannel, suspension,
                        this::performAsyncOperation));
            }
            else {
                performAsyncOperation(FIRST_OPERATION, rChannel, data,
                    new ChannelCallback<>(rChannel, suspension,
                        this::performAsyncOperation));
            }
        }
        catch (Exception e) {
            suspension.fail(e);
        }
    }
}
