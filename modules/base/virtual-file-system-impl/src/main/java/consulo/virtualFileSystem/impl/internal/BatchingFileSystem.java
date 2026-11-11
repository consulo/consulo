// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Implement if your {@link consulo.virtualFileSystem.NewVirtualFileSystem} could benefit from batch queries -- i.e.,
 * if directory's children with their attributes could be collected significantly faster in 1 request than in
 * N individual requests to {@link consulo.virtualFileSystem.NewVirtualFileSystem#getAttributes(VirtualFile)}
 */
public interface BatchingFileSystem {
    /**
     * @return directory's children, in the form of a map of names to file attributes.
     * When the {@code childrenNames} is not-null, only those children's data should be returned (if exists!),
     * otherwise, all directory's children should be returned.
     * The returned map should be a 'normal' (case-sensitive) map – it is up to calling code to covert it to case-insensitive, if needed.
     */
    //Why contract defines returned map to be case-sensitive? Because dir.isCaseSensitive() is tricky, and sometimes it's value could
    // be unreliable/outdated -- in which case this method would provide incorrect info, be it defined to return map with case-sensitivity
    // =dir.isCaseSensitive().
    // But the responsibility of (Batching)FileSystem is _only_ to be golden-source of info about actual file system state -- it is
    // the responsibility of the caller (=VFS) to deal with (possible) inconsistencies between VFS state and actual FS state.
    //MAYBE RC: return List<Pair<String,FileAttributes>>? this way question of case-sensitivity is not even on the table
    Map<String, FileAttributes> listWithAttributes(VirtualFile dir, @Nullable Set<String> childrenNames);

    default Map<String, FileAttributes> listWithAttributes(VirtualFile dir) {
        return listWithAttributes(dir, null);
    }
}
