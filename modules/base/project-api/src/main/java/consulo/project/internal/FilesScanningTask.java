// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.internal;

import org.jspecify.annotations.Nullable;

/**
 * A task in {@link UnindexedFilesScannerExecutor}
 */
public interface FilesScanningTask {
    @Nullable
    Boolean isFullIndexUpdate();

    void close();
}
