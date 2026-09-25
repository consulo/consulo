// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io.stats;

import org.jspecify.annotations.Nullable;

public final class CachedChannelsStatistics {
    private final int myHit;
    private final int myMiss;
    private final int myLoad;
    private final int myBypassedCache;
    private final int myCapacity;

    public CachedChannelsStatistics(int hit,
                                    int miss,
                                    int load,
                                    int bypassedCache,
                                    int capacity) {
        myHit = hit;
        myMiss = miss;
        myLoad = load;
        myBypassedCache = bypassedCache;
        myCapacity = capacity;
    }

    public int getHit() {
        return myHit;
    }

    public int getMiss() {
        return myMiss;
    }

    public int getLoad() {
        return myLoad;
    }

    public int getBypassedCache() {
        return myBypassedCache;
    }

    public int getCapacity() {
        return myCapacity;
    }

    public CachedChannelsStatistics plus(CachedChannelsStatistics other) {
        return new CachedChannelsStatistics(
            myHit + other.myHit,
            myMiss + other.myMiss,
            myLoad + other.myLoad,
            myBypassedCache + other.myBypassedCache,
            myCapacity + other.myCapacity
        );
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CachedChannelsStatistics that)) {
            return false;
        }
        return myHit == that.myHit &&
            myMiss == that.myMiss &&
            myLoad == that.myLoad &&
            myBypassedCache == that.myBypassedCache &&
            myCapacity == that.myCapacity;
    }

    @Override
    public int hashCode() {
        int result = myHit;
        result = 31 * result + myMiss;
        result = 31 * result + myLoad;
        result = 31 * result + myBypassedCache;
        result = 31 * result + myCapacity;
        return result;
    }

    @Override
    public String toString() {
        return "CachedChannelsStatistics(" +
            "hit=" + myHit +
            ", miss=" + myMiss +
            ", load=" + myLoad +
            ", bypassedCache=" + myBypassedCache +
            ", capacity=" + myCapacity +
            ')';
    }
}
