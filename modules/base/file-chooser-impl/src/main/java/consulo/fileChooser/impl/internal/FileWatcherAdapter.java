// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.util.concurrent.coroutine.Channel;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface FileWatcherAdapter {
    CompletableFuture<@Nullable Channel<FileChangeType>> subscribe(Path path);

    CompletableFuture<?> unsubscribe(Path path);

    CompletableFuture<?> stop();
}
