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

import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A file of another machine read where it is - every read asks the agent for that range of the file only, so archive readers
 * (a zip central directory, the index of a jimage) touch just what they need.
 * <p>
 * Small reads come in blocks, a few of which are kept - readers mostly go forward, or back to a header.
 * <p>
 * A {@link FileChannel} - for its reads by position: readers of several threads ({@code JBZipFile}, a jimage) read side by
 * side, not one by one through a shared position. Nothing of it maps, locks or writes.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
class RemoteReadChannel extends FileChannel {
    private static final int BLOCK_SIZE = 64 * 1024;
    private static final int MAX_BLOCKS = 16;

    private final RemoteAgentConnection myConnection;
    private final String myPath;
    private final long mySize;

    private final Map<Long, byte[]> myBlocks = new LinkedHashMap<>(MAX_BLOCKS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
            return size() > MAX_BLOCKS;
        }
    };

    private long myPosition;

    RemoteReadChannel(RemoteAgentConnection connection, String path, long size) {
        myConnection = connection;
        myPath = path;
        mySize = size;
    }

    @Override
    public synchronized int read(ByteBuffer destination) throws IOException {
        int read = read(destination, myPosition);
        if (read > 0) {
            myPosition += read;
        }
        return read;
    }

    /**
     * Reads at the position - of any thread at a time, the position of the channel stays as it is.
     */
    @Override
    public int read(ByteBuffer destination, long position) throws IOException {
        ensureOpen();
        if (position >= mySize) {
            return -1;
        }
        int wanted = (int) Math.min(destination.remaining(), mySize - position);
        if (wanted == 0) {
            return 0;
        }

        if (wanted >= BLOCK_SIZE) {
            // a large read - straight from the agent, no block of it is worth keeping
            byte[] data = readRange(position, wanted);
            if (data.length == 0) {
                return -1;
            }
            destination.put(data);
            return data.length;
        }

        int read = 0;
        long current = position;
        while (read < wanted) {
            long blockStart = (current / BLOCK_SIZE) * BLOCK_SIZE;
            byte[] block = getBlock(blockStart);
            int inBlock = (int) (current - blockStart);
            if (inBlock >= block.length) {
                break;
            }
            int count = Math.min(wanted - read, block.length - inBlock);
            destination.put(block, inBlock, count);
            current += count;
            read += count;
        }
        return read == 0 ? -1 : read;
    }

    @Override
    public long read(ByteBuffer[] destinations, int offset, int length) throws IOException {
        long total = 0;
        for (int i = offset; i < offset + length; i++) {
            int read = read(destinations[i]);
            if (read < 0) {
                return total == 0 ? -1 : total;
            }
            total += read;
            if (destinations[i].hasRemaining()) {
                break;
            }
        }
        return total;
    }

    private byte[] getBlock(long blockStart) throws IOException {
        synchronized (myBlocks) {
            byte[] block = myBlocks.get(blockStart);
            if (block != null) {
                return block;
            }
        }
        int length = (int) Math.min(BLOCK_SIZE, mySize - blockStart);
        byte[] block = readRange(blockStart, length);
        synchronized (myBlocks) {
            myBlocks.put(blockStart, block);
        }
        return block;
    }

    private byte[] readRange(long offset, int length) throws IOException {
        byte[] data = myConnection.readFileRange(myPath, offset, length);
        if (data.length > length) {
            // an agent of before the ranges - it streams the whole file
            throw new IOException("The agent does not read files by ranges, update it: " + myPath);
        }
        return data;
    }

    @Override
    public synchronized long position() throws IOException {
        ensureOpen();
        return myPosition;
    }

    @Override
    public synchronized FileChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException("Negative position: " + newPosition);
        }
        myPosition = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return mySize;
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(count, BLOCK_SIZE));
        long transferred = 0;
        while (transferred < count) {
            buffer.clear().limit((int) Math.min(buffer.capacity(), count - transferred));
            int read = read(buffer, position + transferred);
            if (read <= 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                target.write(buffer);
            }
            transferred += read;
        }
        return transferred;
    }

    // region nothing writes

    @Override
    public int write(ByteBuffer source) {
        throw new NonWritableChannelException();
    }

    @Override
    public long write(ByteBuffer[] sources, int offset, int length) {
        throw new NonWritableChannelException();
    }

    @Override
    public int write(ByteBuffer source, long position) {
        throw new NonWritableChannelException();
    }

    @Override
    public FileChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    public long transferFrom(ReadableByteChannel source, long position, long count) {
        throw new NonWritableChannelException();
    }

    @Override
    public void force(boolean metaData) {
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new IOException("A file of another machine does not map: " + myPath);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new IOException("A file of another machine does not lock: " + myPath);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new IOException("A file of another machine does not lock: " + myPath);
    }

    // endregion

    @Override
    protected void implCloseChannel() {
        synchronized (myBlocks) {
            myBlocks.clear();
        }
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }
}
