// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.projectFilter;

import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.util.lang.Pair;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class ProjectIndexableFilesFilter extends IdFilter {
    private final AtomicVersionedCounter myParallelUpdatesCounter = new AtomicVersionedCounter();

    abstract boolean ensureFileIdPresent(int fileId, BooleanSupplier add);

    abstract void removeFileId(int fileId);

    abstract void resetFileIds();

    void onProjectClosing(Project project, long vfsCreationStamp) {
    }

    /**
     * This is a temp method
     */
    boolean wasDataLoadedFromDisk() {
        return false;
    }

    protected <T> T runUpdate(Supplier<T> action) {
        myParallelUpdatesCounter.update(1);
        try {
            return action.get();
        }
        finally {
            myParallelUpdatesCounter.update(-1);
        }
    }

    private static class AtomicVersionedCounter {
        /** [ counter:int32 << 32 | version:int32 ] */
        private final AtomicLong myCounterAndVersion = new AtomicLong(0L);

        void update(int counterUpdate) {
            while (true) {
                long pair = myCounterAndVersion.get();
                long counter = pair >> 32;
                long version = pair & 0xFFFF_FFFFL;

                counter += counterUpdate;
                if (counter > Integer.MAX_VALUE) {
                    counter = 0;
                }
                version++;
                if (version > Integer.MAX_VALUE) {
                    version = 0;
                }

                long newPair = (counter << 32) | version;
                if (myCounterAndVersion.compareAndSet(pair, newPair)) {
                    break;
                }
            }
        }

        Pair<Integer, Integer> getCounterAndVersion() {
            long pair = myCounterAndVersion.get();
            long counter = pair >> 32;
            long version = pair & 0xFFFF_FFFFL;
            return Pair.create((int)counter, (int)version);
        }
    }
}
