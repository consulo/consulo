// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.logging.Logger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Collection;

public final class OrphanDirtyFilesQueue {
    private static final Logger LOG = Logger.getInstance(OrphanDirtyFilesQueue.class);

    private final IntList myFileIds;
    private final long myUntrimmedSize;

    public OrphanDirtyFilesQueue(IntList fileIds, long untrimmedSize) {
        myFileIds = fileIds;
        myUntrimmedSize = untrimmedSize;
        if (untrimmedSize < fileIds.size()) {
            LOG.error("untrimmedSize must be larger or equal to number of files in orphan queue. fileIds.size=" + fileIds.size()
                + ", untrimmedSize=" + untrimmedSize);
        }
    }

    public IntList getFileIds() {
        return myFileIds;
    }

    public long getUntrimmedSize() {
        return myUntrimmedSize;
    }

    public void store(long vfsVersion) {
        PersistentDirtyFilesQueue.storeIndexingQueue(PersistentDirtyFilesQueue.getQueueFile(), myFileIds, myUntrimmedSize, vfsVersion);
    }

    public OrphanDirtyFilesQueue plus(Collection<Integer> ids) {
        IntList newIds = new IntArrayList(myFileIds);
        newIds.addAll(new IntArrayList(ids));
        return new OrphanDirtyFilesQueue(newIds, myUntrimmedSize + ids.size());
    }

    public OrphanDirtyFilesQueue takeLast(int maxSize) {
        if (maxSize <= 0 || myFileIds.size() <= maxSize) {
            return this;
        }
        return new OrphanDirtyFilesQueue(new IntArrayList(myFileIds.subList(myFileIds.size() - maxSize, myFileIds.size())), myUntrimmedSize);
    }
}
