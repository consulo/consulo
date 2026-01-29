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
import consulo.util.concurrent.coroutine.step.Loop;
import consulo.util.dataholder.Key;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A coroutine step for servers that listens to network request through an
 * instance of {@link AsynchronousServerSocketChannel}. The step will suspend
 * execution until a request is accepted and then spawn another coroutine for
 * the handling of the request. The handling coroutine will receive the
 * {@link AsynchronousSocketChannel} for the communication with the client. It
 * could then store the channel as {@link AsynchronousSocketStep#SOCKET_CHANNEL}
 * and process the request with {@link SocketReceive} and {@link SocketSend}
 * steps.
 *
 * <p>After the request handling coroutine has been spawned this step will
 * resume execution of it's own coroutine. A typical server accept loop
 * therefore needs to be implemented in that coroutine, e.g. inside a
 * conditional {@link Loop} step. Handling coroutines will run in parallel in
 * separate continuations but in the same scope. Interdependencies between the
 * main coroutine and the request handling should be avoided to prevent the risk
 * of deadlocks.</p>
 *
 * @author eso
 */
public class ServerSocketAccept extends AsynchronousChannelStep<Void, Void> {

    /**
     * State: an {@link AsynchronousServerSocketChannel} that has been openened
     * and connected by an asynchronous execution.
     */
    public static final Key<AsynchronousServerSocketChannel> SERVER_SOCKET_CHANNEL = Key.create(AsynchronousServerSocketChannel.class);

    private final Function<Continuation<?>, SocketAddress> getSocketAddress;

    private final Coroutine<AsynchronousSocketChannel, ?> requestHandler;

    /**
     * Creates a new instance that accepts a single server request and processes
     * it asynchronously in a coroutine. The server socket is bound to the local
     * socket with the address provided by the given factory. The factory may
     * return NULL if the step should connect to a channel that is stored in a
     * state relation with the type {@link #SERVER_SOCKET_CHANNEL}.
     *
     * @param getSocketAddress A function that provides the socket address to
     *                         connect to from the current continuation
     * @param requestHandler   A coroutine that processes a single server
     *                         request
     */
    public ServerSocketAccept(
        Function<Continuation<?>, SocketAddress> getSocketAddress,
        Coroutine<AsynchronousSocketChannel, ?> requestHandler) {
        Objects.requireNonNull(getSocketAddress);

        this.getSocketAddress = getSocketAddress;
        this.requestHandler = requestHandler;
    }

    /**
     * Accepts an incoming request on the given socket address and then handles
     * it by executing the given coroutine with the client socket channel as
     * it's input.
     *
     * @param getSocketAddress A function that produces the address of the local
     *                         address to accept the request from
     * @param requestHandler   The coroutine to process the next request with
     * @return The new step
     */
    public static ServerSocketAccept acceptRequestOn(
        Function<Continuation<?>, SocketAddress> getSocketAddress,
        Coroutine<AsynchronousSocketChannel, ?> requestHandler) {
        return new ServerSocketAccept(getSocketAddress, requestHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAsync(CompletableFuture<Void> previousExecution,
                         CoroutineStep<Void, ?> nextStep, Continuation<?> continuation) {
        continuation.continueAccept(previousExecution,
            v -> acceptAsync(continuation.suspend(this, nextStep)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void execute(Void input, Continuation<?> continuation) {
        try {
            AsynchronousServerSocketChannel rChannel =
                getServerSocketChannel(continuation);

            requestHandler.runBlocking(continuation.scope(),
                rChannel.accept().get());
        }
        catch (Exception e) {
            throw new CoroutineException(e);
        }

        return null;
    }

    /**
     * Returns the channel to be used by this step. This first checks the
     * currently exexcuting coroutine in the continuation parameter for an
     * existing {@link #SERVER_SOCKET_CHANNEL} relation. If that doesn't exists
     * or if it contains a closed channel a new
     * {@link AsynchronousServerSocketChannel} will be opened and stored in the
     * state object.
     *
     * @param continuation The continuation to query for an existing channel
     * @return The channel
     * @throws IOException If opening the channel fails
     */
    protected AsynchronousServerSocketChannel getServerSocketChannel(
        Continuation<?> continuation) throws IOException {
        Coroutine<?, ?> coroutine = continuation.getCurrentCoroutine();

        AsynchronousServerSocketChannel channel = coroutine.getUserData(SERVER_SOCKET_CHANNEL);

        if (channel == null || !channel.isOpen()) {
            channel = AsynchronousServerSocketChannel.open(getChannelGroup(continuation));

            coroutine.putUserData(SERVER_SOCKET_CHANNEL, channel);

            AutoClosableRegister.register(coroutine, channel);
        }

        if (channel.getLocalAddress() == null) {
            channel.bind(getSocketAddress(continuation));
        }

        return channel;
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
     * Opens and connects a {@link Channel} to the {@link SocketAddress} of this
     * step and then performs the channel operation asynchronously.
     *
     * @param suspension The coroutine suspension to be resumed when the
     *                   operation is complete
     */
    private void acceptAsync(Suspension<Void> suspension) {
        try {
            AsynchronousServerSocketChannel channel =
                getServerSocketChannel(suspension.continuation());

            channel.accept(null,
                new AcceptCallback(requestHandler, suspension));
        }
        catch (Exception e) {
            suspension.fail(e);
        }
    }

    /**
     * A {@link CompletionHandler} implementation that receives the result of an
     * asynchronous accept and processes the request with an asynchronous
     * coroutine execution.
     *
     * @author eso
     */
    protected static class AcceptCallback
        implements CompletionHandler<AsynchronousSocketChannel, Void> {

        private final Coroutine<AsynchronousSocketChannel, ?> requestHandler;

        private final Suspension<Void> suspension;

        /**
         * Creates a new instance.
         *
         * @param requestHandler The coroutine to process the request with
         * @param suspension     The suspension to resume after accepting
         */
        public AcceptCallback(
            Coroutine<AsynchronousSocketChannel, ?> requestHandler,
            Suspension<Void> suspension) {
            this.requestHandler = requestHandler;
            this.suspension = suspension;
        }

        @Override
        public void completed(AsynchronousSocketChannel requestChannel,
                              Void ignored) {
            requestHandler.runAsync(suspension.continuation().scope(),
                requestChannel);

            suspension.resume();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void failed(Throwable error, Void ignored) {
            suspension.fail(error);
        }
    }
}
