// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Optional diagnostics for cache-backed accessors, used by storage lifecycle checks.
 */
interface DiagnosticChannelsAccessor {
    /**
     * Describes a cached channel for the path, or returns {@code null} if this accessor has no such channel open.
     */
    @Nullable String describeCachedChannelOrNull(Path path);
}
