// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.project.Project;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Collection;

public final class ProjectDirtyFilesQueue {
    private final Collection<Integer> myFileIds;
    private final long myLastSeenIndexInOrphanQueue;

    public ProjectDirtyFilesQueue(Collection<Integer> fileIds, long lastSeenIndexInOrphanQueue) {
        myFileIds = fileIds;
        myLastSeenIndexInOrphanQueue = lastSeenIndexInOrphanQueue;
    }

    public Collection<Integer> getFileIds() {
        return myFileIds;
    }

    public long getLastSeenIndexInOrphanQueue() {
        return myLastSeenIndexInOrphanQueue;
    }

    public void store(Project project, long vfsVersion) {
        PersistentDirtyFilesQueue.storeIndexingQueue(
            PersistentDirtyFilesQueue.getQueueFile(project),
            new IntArrayList(myFileIds),
            myLastSeenIndexInOrphanQueue,
            vfsVersion
        );
    }
}
