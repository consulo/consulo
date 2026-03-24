// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.annotate;

import consulo.versionControlSystem.VcsException;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

/**
 * Extension of {@link AnnotationProvider} that supports an in-memory annotation cache.
 * <p>
 * Implementations should populate an in-memory cache via {@link #populateCache} (called by
 * {@link consulo.versionControlSystem.impl.internal.annotate.AnnotationsPreloader}) and expose it
 * via {@link #getFromCache} for non-blocking retrieval by the Code Vision infrastructure.
 *
 * @see consulo.versionControlSystem.impl.internal.annotate.AnnotationsPreloader
 */
public interface CacheableAnnotationProvider {
    /**
     * Populates the in-memory annotation cache for the given file.
     * May perform I/O; must be called off the EDT.
     */
    void populateCache(VirtualFile file) throws VcsException;

    /**
     * Returns the cached annotation for the given file, or {@code null} if not yet cached.
     * Must be fast (no I/O).
     */
    @Nullable
    FileAnnotation getFromCache(VirtualFile file);
}
