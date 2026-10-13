/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.remoteServer.agent.impl.internal.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

class RemoteByteChannel implements SeekableByteChannel {
    @FunctionalInterface
    interface Flusher {
        void flush(byte[] content) throws IOException;
    }

    private final Flusher myFlusher;
    private final boolean myWritable;

    private byte[] myContent;
    private int mySize;
    private int myPosition;
    private boolean myModified;
    private boolean myOpen = true;

    RemoteByteChannel(byte[] content, boolean writable, boolean append, Flusher flusher) {
        myContent = content;
        mySize = content.length;
        myWritable = writable;
        myFlusher = flusher;
        myPosition = append ? mySize : 0;
    }

    @Override
    public int read(ByteBuffer destination) throws IOException {
        ensureOpen();
        if (myPosition >= mySize) {
            return -1;
        }
        int count = Math.min(destination.remaining(), mySize - myPosition);
        destination.put(myContent, myPosition, count);
        myPosition += count;
        return count;
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        ensureOpen();
        if (!myWritable) {
            throw new NonWritableChannelException();
        }
        int count = source.remaining();
        ensureCapacity(myPosition + count);
        source.get(myContent, myPosition, count);
        myPosition += count;
        mySize = Math.max(mySize, myPosition);
        myModified = true;
        return count;
    }

    @Override
    public long position() throws IOException {
        ensureOpen();
        return myPosition;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position out of range: " + newPosition);
        }
        myPosition = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return mySize;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        ensureOpen();
        if (!myWritable) {
            throw new NonWritableChannelException();
        }
        if (size < mySize) {
            mySize = (int) size;
            myModified = true;
        }
        myPosition = Math.min(myPosition, mySize);
        return this;
    }

    @Override
    public boolean isOpen() {
        return myOpen;
    }

    @Override
    public void close() throws IOException {
        if (!myOpen) {
            return;
        }
        myOpen = false;
        if (myModified) {
            myFlusher.flush(Arrays.copyOf(myContent, mySize));
        }
    }

    private void ensureCapacity(int capacity) {
        if (capacity > myContent.length) {
            myContent = Arrays.copyOf(myContent, Math.max(capacity, myContent.length * 2));
        }
    }

    private void ensureOpen() throws IOException {
        if (!myOpen) {
            throw new ClosedChannelException();
        }
    }
}
