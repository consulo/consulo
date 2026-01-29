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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Implements asynchronous writing to a {@link AsynchronousFileChannel}.
 *
 * @author eso
 */
public class FileWrite extends AsynchronousFileStep {

    /**
     * Creates a new instance.
     *
     * @param getFileChannel A function that provides the file channel from the
     *                       current continuation
     */
    public FileWrite(
        Function<Continuation<?>, AsynchronousFileChannel> getFileChannel) {
        super(getFileChannel);
    }

    /**
     * Suspends until all data from the input {@link ByteBuffer} has been
     * written to a file.The buffer must be initialized for sending, i.e. if
     * necessary a call to {@link Buffer#flip()} must have been performed.
     *
     * <p>After the data has been fully written {@link ByteBuffer#clear()} will
     * be invoked on the buffer so that it can be used directly for subsequent
     * writing to it.</p>
     *
     * @param getFileChannel A function that provides the file channel from the
     *                       current continuation
     * @return A new step instance
     */
    public static FileWrite writeTo(
        Function<Continuation<?>, AsynchronousFileChannel> getFileChannel) {
        return new FileWrite(getFileChannel);
    }

    /**
     * Invokes {@link #writeTo(Function)} with a function that opens a file
     * channel with the given file name and options. The option
     * {@link StandardOpenOption#WRITE} will always be used and should therefore
     * not occur in the extra options.
     *
     * @param fileName     The name of the file to read from
     * @param extraOptions Additional options to use besides
     *                     {@link StandardOpenOption#WRITE}
     * @return A new step instance
     */
    public static FileWrite writeTo(String fileName,
                                    OpenOption... extraOptions) {
        return writeTo(c -> openFileChannel(fileName, StandardOpenOption.WRITE,
            extraOptions));
    }

    @Override
    protected boolean performAsyncOperation(int bytesWritten,
                                            AsynchronousFileChannel channel, ByteBuffer data,
                                            ChannelCallback<Integer, AsynchronousFileChannel> callback) {
        long position = Objects.requireNonNullElse(getUserData(FILE_POSITION), 0L);

        if (data.hasRemaining()) {
            if (bytesWritten > 0) {
                position += bytesWritten;
            }

            channel.write(data, position, data, callback);

            return false;
        }
        else // finished
        {
            // remove position in the case of a later restart
            putUserData(FILE_POSITION, null);
            data.clear();

            return true;
        }
    }

    @Override
    protected void performBlockingOperation(AsynchronousFileChannel channel,
                                            ByteBuffer data) throws InterruptedException, ExecutionException {
        long position = 0;

        while (data.hasRemaining()) {
            position += channel.write(data, position).get();
        }

        data.clear();
    }
}
