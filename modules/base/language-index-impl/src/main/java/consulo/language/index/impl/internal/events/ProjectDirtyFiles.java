// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.events;

import consulo.application.progress.PingProgress;
import consulo.util.collection.ConcurrentBitSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Collection;

/**
 * Per-Project dirty files.
 * TODO RC: actually, it is better named just DirtyFiles, since 1) it doesn't contain a project ref, and 2) used
 *          for 'unknown project dirty files' also. While current DirtyFiles better be named PerProjectDirtyFiles
 */
public final class ProjectDirtyFiles {
    //TODO RC: using CBS for fileId is not very memory-efficient, because the fileId could be quite large, and CBS is forced to
    //         allocate a lot of memory for nothing
    private final ConcurrentBitSet myFilesSet = new ConcurrentBitSet();

    public boolean addFile(int fileId) {
        return myFilesSet.set(fileId);
    }

    public boolean containsFile(int fileId) {
        return myFilesSet.get(fileId);
    }

    public boolean removeFile(int fileId) {
        return myFilesSet.clear(fileId);
    }

    public void clear() {
        myFilesSet.clear();
    }

    public void addFiles(Collection<Integer> fileIds) {
        for (int fileId : fileIds) {
            addFile(fileId);
        }
    }

    public void removeFiles(Collection<Integer> fileIds) {
        for (int fileId : fileIds) {
            removeFile(fileId);
        }
    }

    public void addAllTo(IntSet set) {
        for (int fileId = 0; fileId < myFilesSet.size(); fileId++) {
            if (myFilesSet.get(fileId)) {
                PingProgress.interactWithEdtProgress();
                set.add(fileId);
            }
        }
    }
}
