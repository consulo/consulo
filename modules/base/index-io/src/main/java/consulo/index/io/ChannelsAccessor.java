// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.index.io.FileChannelInterruptsRetryer.FileChannelIdempotentOperation;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Abstracts different ways of caching/not caching opened {@linkplain FileChannel}s
 */
public interface ChannelsAccessor {
    boolean isReadOnly();

    <T> T executeOp(Path path,
                    FileChannelOperation<T> operation) throws IOException;

    <T> T executeIdempotentOp(Path path,
                              FileChannelIdempotentOperation<T> operation) throws IOException;

    void closeChannel(Path path) throws IOException;
}
