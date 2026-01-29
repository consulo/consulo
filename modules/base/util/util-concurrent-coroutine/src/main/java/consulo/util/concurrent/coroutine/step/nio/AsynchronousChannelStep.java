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
import consulo.util.dataholder.Key;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * The base class for coroutine steps that perform communication through
 * instances of {@link AsynchronousChannel}. It contains the inner class
 * {@link ChannelCallback} that implements most of the {@link CompletionHandler}
 * interface needed for asynchonous channel communication. The actual channel
 * operation must be provided to it as an implementation of the function
 * interface {@link ChannelOperation}.
 *
 * <p>To simplify the generic declaration of subclasses both input and output
 * type are declared as {@link ByteBuffer}, where the returned value will be the
 * input value. Input buffers must be provided by the preceding step in a
 * coroutine and initialize it for the respective channel step implemenation.
 * For a reading step this means that the buffer must have an adequate capacity.
 * For a writing step it must contain the data to write and it must have been
 * flipped if necessary (see {@link Buffer#flip()} for details).</p>
 *
 * @author eso
 */
public abstract class AsynchronousChannelStep<I, O> extends CoroutineStep<I, O> {
    /**
     * State: the {@link AsynchronousChannelGroup} to associate any new
     * asynchronous channels with.
     */
    public static final Key<AsynchronousChannelGroup> CHANNEL_GROUP = Key.create(AsynchronousChannelGroup.class);

    /**
     * Internal signal for the first operation after a connection.
     */
    static final int FIRST_OPERATION = -2;

    /**
     * Returns the {@link AsynchronousChannelGroup} for asynchronous channel
     * operations in the current scope. If no such group exists a new one will
     * be created with the {@link ExecutorService} of the
     * {@link CoroutineContext} and stored as {@link #CHANNEL_GROUP} in the
     * current scope.
     *
     * @param continuation The channel group
     * @return The channel group
     */
    protected AsynchronousChannelGroup getChannelGroup(
        Continuation<?> continuation) {
        AsynchronousChannelGroup channelGroup = continuation.getUserData(CHANNEL_GROUP);

        if (channelGroup == null) {
            Executor rContextExecutor = continuation.context().getExecutor();

            if (rContextExecutor instanceof ExecutorService) {
                try {
                    channelGroup = AsynchronousChannelGroup.withThreadPool(
                        (ExecutorService) rContextExecutor);
                }
                catch (IOException e) {
                    throw new CoroutineException(e);
                }

                continuation.scope().putUserData(CHANNEL_GROUP, channelGroup);
            }
        }

        return channelGroup;
    }

    /**
     * A functional interface used as argument to {@link ChannelCallback}.
     *
     * @author eso
     */
    @FunctionalInterface
    protected interface ChannelOperation<C extends AsynchronousChannel> {
        /**
         * Performs an asnychronous channel operation if necessary or returns
         * FALSE.
         *
         * @param bytesProcessed The number of bytes that have been processed by
         *                       a previous invocation
         * @param channel        The channel to perform the operation on
         * @param data           The byte buffer for the operation data
         * @param callback       The callback to be invoked (recursively) upon
         *                       completion of the operation
         * @return FALSE if a recursive asynchronous execution has been started,
         * TRUE if the operation is complete
         * @throws Exception Any kind of exception may be thrown
         */
        boolean execute(int bytesProcessed, C channel, ByteBuffer data,
                        ChannelCallback<Integer, C> callback) throws Exception;
    }

    /**
     * A {@link CompletionHandler} implementation that performs an asynchronous
     * channel operation and resumes a coroutine step afterwards
     * (asynchronously).
     *
     * @author eso
     */
    protected static class ChannelCallback<V, C extends AsynchronousChannel>
        implements CompletionHandler<V, ByteBuffer> {
        private final C channel;

        private final Suspension<ByteBuffer> suspension;

        private final ChannelOperation<C> operation;

        /**
         * Creates a new instance.
         *
         * @param channel    The channel to operate on
         * @param suspension The suspension to be resumed when the operation is
         *                   completed
         * @param operation  The asynchronous channel operation to perform
         */
        protected ChannelCallback(C channel, Suspension<ByteBuffer> suspension,
                                  ChannelOperation<C> operation) {
            this.channel = channel;
            this.suspension = suspension;
            this.operation = operation;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void completed(V result, ByteBuffer data) {
            // first invocation from connect is Void, then read/write integers
            int processed = result instanceof Integer ?
                ((Integer) result).intValue() :
                FIRST_OPERATION;

            try {
                // required to force THIS to integer to have only one implementation
                // as the Java NIO API declares the connect stage callback as Void
                if (operation.execute(processed, channel, data,
                    (ChannelCallback<Integer, C>) this)) {
                    suspension.resume(data);
                }
            }
            catch (Exception e) {
                suspension.fail(e);
            }
        }

        @Override
        public void failed(Throwable error, ByteBuffer data) {
            suspension.fail(error);
        }
    }
}
