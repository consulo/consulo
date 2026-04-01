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
package consulo.virtualFileSystem.nativeFileType.impl.internal;

import consulo.util.io.ByteSequence;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * @author VISTALL
 * @since 2026-03-31
 */
class ByteSequenceSeekableByteChannel implements SeekableByteChannel {
    private final ByteSequence myBytes;
    private int myPosition;

    ByteSequenceSeekableByteChannel(ByteSequence bytes) {
        myBytes = bytes;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int remaining = myBytes.length() - myPosition;
        if (remaining <= 0) {
            return -1;
        }
        int count = Math.min(remaining, dst.remaining());
        for (int i = 0; i < count; i++) {
            dst.put(myBytes.byteAt(myPosition++));
        }
        return count;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        return myPosition;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        myPosition = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return myBytes.length();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }
}
