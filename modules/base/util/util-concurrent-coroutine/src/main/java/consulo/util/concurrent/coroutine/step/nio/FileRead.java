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
import consulo.util.concurrent.coroutine.CoroutineException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Implements asynchronous reading from a {@link AsynchronousFileChannel}.
 *
 * @author eso
 */
public class FileRead extends AsynchronousFileStep {

    private final BiPredicate<Integer, ByteBuffer> checkFinished;

    /**
     * Creates a new instance.
     *
     * @param getFileChannel A function that provides the file channel from the
     *                       current continuation
     * @param checkFinished  A predicate that checks whether the operation is
     *                       complete by evaluating the byte buffer after
     *                       reading
     */
    public FileRead(
        Function<Continuation<?>, AsynchronousFileChannel> getFileChannel,
        BiPredicate<Integer, ByteBuffer> checkFinished) {
        super(getFileChannel);

        this.checkFinished = checkFinished;
    }

    /**
     * Suspends until a file has been read completely. The data will be stored
     * in the input {@link ByteBuffer} of the step. If the capacity of the
     * buffer is reached before the EOF signal is received the coroutine will be
     * terminated with a {@link CoroutineException}. To stop reading when a
     * certain condition is met a derived step can be created with
     * {@link #until(BiPredicate)}.
     *
     * <p>After the data has been fully received {@link ByteBuffer#flip()} will
     * be invoked on the buffer so that it can be used directly for subsequent
     * reading from it.</p>
     *
     * @param getFileChannel A function that provides the file channel from the
     *                       current continuation
     * @return A new step instance
     */
    public static AsynchronousFileStep readFrom(
        Function<Continuation<?>, AsynchronousFileChannel> getFileChannel) {
        return new FileRead(getFileChannel, (r, bb) -> r != -1);
    }

    /**
     * Invokes {@link #readFrom(Function)} with a function that opens a file
     * channel with the given file name and options. The option
     * {@link StandardOpenOption#READ} will always be used and should therefore
     * not occur in the extra options.
     *
     * @param fileName     The name of the file to read from
     * @param extraOptions Additional options to use besides
     *                     {@link StandardOpenOption#READ}
     * @return A new step instance
     */
    public static AsynchronousFileStep readFrom(String fileName,
                                                OpenOption... extraOptions) {
        return readFrom(c -> openFileChannel(fileName, StandardOpenOption.READ,
            extraOptions));
    }

    /**
     * Returns a new read step instance the suspends until data has been read
     * from a file and a certain condition on that data is met or an
     * end-of-stream signal is received. If the capacity of the buffer is
     * reached before the receiving is finished the coroutine will fail with an
     * exception.
     *
     * @param checkFinished A predicate that checks whether the data has been
     *                      read completely
     * @return A new step instance
     */
    public AsynchronousFileStep until(
        BiPredicate<Integer, ByteBuffer> checkFinished) {
        return new FileRead(getFileChannelFactory(), checkFinished);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean performAsyncOperation(int bytesRead,
                                            AsynchronousFileChannel channel, ByteBuffer data,
                                            ChannelCallback<Integer, AsynchronousFileChannel> callback)
        throws IOException {
        long position = Objects.requireNonNullElse(getUserData(FILE_POSITION), 0L);
        boolean finished = false;

        if (bytesRead >= 0) {
            finished = checkFinished.test(bytesRead, data);
            position += bytesRead;
        }

        if (bytesRead != -1 && !finished && data.hasRemaining()) {
            channel.read(data, position, data, callback);
        }
        else // finished, either successfully or with an error
        {
            checkErrors(data, bytesRead, finished);

            // remove position in the case of a later restart
            putUserData(FILE_POSITION, null);

            data.flip();
        }

        return finished;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBlockingOperation(AsynchronousFileChannel channel,
                                            ByteBuffer data) throws Exception {
        long nPosition = 0;
        int bytesRead;
        boolean finished;

        do {
            bytesRead = channel.read(data, nPosition).get();
            finished = checkFinished.test(bytesRead, data);

            if (bytesRead > 0) {
                nPosition += bytesRead;
            }
        }
        while (bytesRead != -1 && !finished && data.hasRemaining());

        checkErrors(data, bytesRead, finished);

        data.flip();
    }

    /**
     * Checks the read data and throws an exception on errors.
     *
     * @param data      The received data bytes
     * @param bytesRead The number of bytes in the last read
     * @param finished  TRUE if the finish condition is met
     * @throws IOException If an error is detected
     */
    private void checkErrors(ByteBuffer data, int bytesRead, boolean finished)
        throws IOException {
        if (!finished) {
            if (bytesRead == -1) {
                throw new IOException("Received data incomplete");
            }
            else if (!data.hasRemaining()) {
                throw new IOException("Buffer capacity exceeded");
            }
        }
    }
}
