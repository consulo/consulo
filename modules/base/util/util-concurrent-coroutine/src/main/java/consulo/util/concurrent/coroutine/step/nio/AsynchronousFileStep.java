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

import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.coroutine.*;
import consulo.util.concurrent.coroutine.internal.AutoClosableRegister;
import consulo.util.dataholder.Key;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * The base class for coroutine steps that perform communication on instances of
 * {@link AsynchronousFileChannel}. The channel will be opened and connected as
 * necessary. It may also be provided before the step is invoked in a state
 * relation with the type {@link #FILE_CHANNEL}. If the channel is opened by
 * this step it will have the {@link MetaTypes#MANAGED} flag so that it will be
 * automatically closed when the {@link CoroutineScope} finishes.
 *
 * @author eso
 */
public abstract class AsynchronousFileStep
    extends AsynchronousChannelStep<ByteBuffer, ByteBuffer> {

    /**
     * State: the {@link AsynchronousFileChannel} that the steps in a coroutine
     * operate on.
     */
    public static final Key<AsynchronousFileChannel> FILE_CHANNEL = Key.create(AsynchronousFileChannel.class);

    /**
     * The current position when reading from or writing to a file channel. Can
     * also be set in advance to start reading from that position instead of the
     * file start.
     */
    public static final Key<Long> FILE_POSITION = Key.create("AsynchronousFileStep#FILE_POSITION");


    private final Function<Continuation<?>, AsynchronousFileChannel>
        getFileChannel;

    /**
     * Creates a new instance that opens a file channel with the given factory.
     * The factory may return NULL if the step should use a file channel that is
     * stored in a state relation with the type {@link #FILE_CHANNEL}.
     *
     * @param getFileChannel A function that opens a file channel for the
     *                       current continuation
     */
    public AsynchronousFileStep(
        Function<Continuation<?>, AsynchronousFileChannel> getFileChannel) {
        Objects.requireNonNull(getFileChannel);

        this.getFileChannel = getFileChannel;
    }

    /**
     * A helper function that opens a file channel for a certain file name and
     * open options.
     *
     * @param fileName     The file name
     * @param mode         The open option for the file access mode (e.g. READ,
     *                     WRITE)
     * @param extraOptions Optional extra file open options
     * @return The file channel
     */
    protected static AsynchronousFileChannel openFileChannel(String fileName,
                                                             OpenOption mode, OpenOption... extraOptions) {
        try {
            return AsynchronousFileChannel.open(new File(fileName).toPath(),
                ArrayUtil.append(extraOptions, mode));
        }
        catch (IOException e) {
            throw new CoroutineException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAsync(CompletableFuture<ByteBuffer> previousExecution,
                         CoroutineStep<ByteBuffer, ?> nextStep, Continuation<?> continuation) {
        continuation.continueAccept(previousExecution,
            b -> transferAsync(b, continuation.suspend(this, nextStep)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ByteBuffer execute(ByteBuffer input,
                                 Continuation<?> continuation) {
        try {
            AsynchronousFileChannel rChannel = getFileChannel(continuation);

            performBlockingOperation(rChannel, input);
        }
        catch (Exception e) {
            throw new CoroutineException(e);
        }

        return input;
    }

    /**
     * Returns the channel to be used by this step. This first checks the
     * currently exexcuting coroutine in the continuation parameter for an
     * existing {@link #FILE_CHANNEL} relation. If that doesn't exists or the
     * channel is closed a new {@link AsynchronousFileChannel} will be opened
     * and stored in the coroutine relation. Using the coroutine to store the
     * channel allows coroutines to be structured so that multiple subroutines
     * perform communication on different channels.
     *
     * @param continuation The continuation to query for an existing channel
     * @return The socket channel
     * @throws IOException If opening the channel fails
     */
    protected AsynchronousFileChannel getFileChannel(
        Continuation<?> continuation) throws IOException {
        Coroutine<?, ?> rCoroutine = continuation.getCurrentCoroutine();

        AsynchronousFileChannel rChannel = rCoroutine.getUserData(FILE_CHANNEL);

        if (rChannel == null || !rChannel.isOpen()) {
            rChannel = getFileChannel.apply(continuation);
            rCoroutine.putUserData(FILE_CHANNEL, rChannel);

            AutoClosableRegister.register(rCoroutine, rChannel);
        }

        return rChannel;
    }

    /**
     * Returns the file channel factory of this step.
     *
     * @return The file channel factory function
     */
    protected Function<Continuation<?>, AsynchronousFileChannel> getFileChannelFactory() {
        return getFileChannel;
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
                                                     AsynchronousFileChannel channel, ByteBuffer data,
                                                     ChannelCallback<Integer, AsynchronousFileChannel> callback)
        throws Exception;

    /**
     * Must be implemented for the blocking execution of a step. It receives an
     * {@link AsynchronousFileChannel} which must be accessed through the
     * blocking API (like {@link Future#get()}).
     *
     * @param channel The channel to perform the operation on
     * @param data    The byte buffer for the operation data
     * @throws Exception Any kind of exception may be thrown
     */
    protected abstract void performBlockingOperation(
        AsynchronousFileChannel channel, ByteBuffer data) throws Exception;

    /**
     * Opens a {@link AsynchronousFileChannel} and then performs the channel
     * operation asynchronously.
     *
     * @param data       The byte buffer of the data to be processed
     * @param suspension The coroutine suspension to be resumed when the
     *                   operation is complete
     */
    private void transferAsync(ByteBuffer data,
                               Suspension<ByteBuffer> suspension) {
        try {
            AsynchronousFileChannel channel =
                getFileChannel(suspension.continuation());

            performAsyncOperation(FIRST_OPERATION, channel, data,
                new ChannelCallback<>(channel, suspension,
                    this::performAsyncOperation));
        }
        catch (Exception e) {
            suspension.fail(e);
        }
    }
}
