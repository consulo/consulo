// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Implement if your {@link NewVirtualFileSystem} can tell an unavailable environment from a missing file.
 * <p>
 * The methods have the contracts of {@link NewVirtualFileSystem#getAttributes} and {@link BatchingFileSystem#listWithAttributes}, with two differences.
 * When the environment behind the path is not connected, they throw {@link FileSystemUnavailableException}; they do not answer "absent" or "empty".
 * And they are not cancellable: the I/O runs on the calling thread, so a cancelled progress waits for it.
 * Call them from a non-cancellable section, see {@link consulo.application.progress.ProgressManager#executeNonCancelableSection}.
 * <p>
 * The VFS refresh uses them: it keeps a file it cannot read dirty, with no events, until the environment answers again.
 */
public interface UnavailabilityAwareFileSystem {
    /**
     * @return the attributes of {@code file}, or {@code null} if the file does not exist.
     */
    @Nullable FileAttributes getAttributesOrThrowIfUnavailable(VirtualFile file) throws FileSystemUnavailableException;

    /**
     * @param names has the meaning of {@code childrenNames} in {@link BatchingFileSystem#listWithAttributes}.
     * @return the children of {@code dir} with their attributes.
     */
    Map<String, FileAttributes> listWithAttributesOrThrowIfUnavailable(VirtualFile dir, @Nullable Set<String> names)
        throws FileSystemUnavailableException;
}
