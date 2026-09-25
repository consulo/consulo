// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.index.io.FileChannelInterruptsRetryer.FileChannelIdempotentOperation;

import java.io.IOException;

public interface Resilient {
    /**
     * Executes idempotent (=safely repeatable) operation on the channel, retrying the operation until succeeded
     */
    <T> T executeOperation(FileChannelIdempotentOperation<T> operation) throws IOException;
}
