// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.language.index.impl.internal.perFileVersion.AutoRefreshingOnVfsCloseRef;
import consulo.language.index.impl.internal.perFileVersion.IntFileAttribute;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A file has three indexed states (per particular index): indexed (with particular index_stamp which monotonically increases),
 * outdated and (trivial) unindexed.
 * <ul>
 *   <li>If the index version is advanced, or we rebuild it, then index_stamp is advanced, we rebuild everything.</li>
 *   <li>If we get a remove file event, then we should remove all indexed state from indices data for it (if state is nontrivial)
 *  * and set its indexed state to outdated.</li>
 *   <li>If we get another event we set indexed state to outdated.</li>
 * </ul>
 *
 * IndexingStamp contains only _indexes_ modification count for each file, it doesn't keep _file_ modification stamp.
 * It means it can become outdated if a file was changed but the {@link Timestamps} for the given file was not updated, or not
 * flushed on disk before IDE was terminated.
 * In such cases {@link IndexingFlag} is used to determine that a file needs to be re-indexed: {@link IndexingFlag} contains
 * file modification count when it was last indexed.
 * So actual state (freshness) of the given fileId data in indexes is defined by both {@link IndexingStamp} and {@link IndexingFlag},
 * _together_.
 */
public final class IndexingStamp {
    public static final long INDEX_DATA_OUTDATED_STAMP = -2L;
    public static final long HAS_NO_INDEXED_DATA_STAMP = 0L;

    static final int INVALID_FILE_ID = 0;

    private IndexingStamp() {
    }

    public static FileIndexingStateWithExplanation isFileIndexedStateCurrent(int fileId, ID<?, ?> indexName) {
        try {
            long stamp = getIndexStamp(fileId, indexName);
            if (stamp == HAS_NO_INDEXED_DATA_STAMP) {
                return FileIndexingStateWithExplanation.notIndexed();
            }
            long indexCreationStamp = IndexVersion.getIndexCreationStamp(indexName);
            return stamp == indexCreationStamp
                ? FileIndexingStateWithExplanation.upToDate()
                : FileIndexingStateWithExplanation.outdated(() -> "stamp(" + stamp + ") != indexCreationStamp(" + indexCreationStamp + ")");
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                // in case of IO exceptions, consider the file unindexed
                return FileIndexingStateWithExplanation.outdated("RuntimeException caused by IOException");
            }
            throw e;
        }
    }

    public static void setFileIndexedStateCurrent(int fileId, ID<?, ?> id, boolean isProvidedByInfrastructureExtension) {
        // TODO-ank: use isProvidedByInfrastructureExtension (DEA-334413)
        update(fileId, id, IndexVersion.getIndexCreationStamp(id));
    }

    public static void setFileIndexedStateOutdated(int fileId, ID<?, ?> id) {
        update(fileId, id, INDEX_DATA_OUTDATED_STAMP);
    }

    public static void setFileIndexedStateUnindexed(int fileId, ID<?, ?> id) {
        update(fileId, id, HAS_NO_INDEXED_DATA_STAMP);
    }

    private static final int INDEXING_STAMP_CACHE_CAPACITY = SystemProperties.getIntProperty("index.timestamp.cache.size", 100);

    //MAYBE RC: do we still need in-memory cache (fileId->Timestamps)? With new fast-attributes + fast enumerator
    //          access may be fast enough even without caching -- or, at least, it may be worth caching enumerator
    //          records (which are 100-1000 records at max) _only_
    private static final ConcurrentIntObjectMap<Timestamps> ourTimestampsCache = IntMaps.newConcurrentIntObjectHashMap();
    private static final BlockingQueue<Integer> ourFinishedFiles = new ArrayBlockingQueue<>(INDEXING_STAMP_CACHE_CAPACITY);

    /**
     * The lock protects reading/modifying the {@link Timestamps} state for fileId.
     * It doesn't protect {@link #ourTimestampsCache} -- it is a concurrent map itself, doesn't need protection.
     */
    private static final StripedLock ourTimestampsPerFileLock = new StripedLock();

    private static final AutoRefreshingOnVfsCloseRef<IndexingStampStorage> ourStorage =
        new AutoRefreshingOnVfsCloseRef<>(IndexingStamp::createStorage);

    // Read lock is used to flush caches. Write lock is to wait until all threads have finished flushing.
    // This is kind of abuse of RW lock. The goal is to allow concurrent execution of flushCache(int finishedFile) from different threads.
    private static final ReadWriteLock ourFlushLock = new ReentrantReadWriteLock();

    private static IndexingStampStorage createStorage(FSRecordsProxy vfs) throws IOException {
        if (IntFileAttribute.shouldUseFastAttributes()) {
            return new IndexingStampStorageOverFastAttributes(vfs);
        }
        else {
            return new IndexingStampStorageOverRegularAttributes(vfs);
        }
    }

    private static IndexingStampStorage storage() {
        try {
            return ourStorage.get();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TestOnly
    public static void dropTimestampMemoryCaches() {
        flushCaches();
        ourTimestampsCache.clear();
    }

    public static long getIndexStamp(int fileId, ID<?, ?> indexName) {
        return ourTimestampsPerFileLock.withReadLock(fileId, () -> {
            Timestamps stamp = createOrGetTimeStamp(fileId);
            return stamp.get(indexName);
        });
    }

    @TestOnly
    public static void dropIndexingTimeStamps(int fileId) throws IOException {
        ourTimestampsCache.remove(fileId);
        storage().writeTimestamps(fileId, TimestampsImmutable.EMPTY);
    }

    private static Timestamps createOrGetTimeStamp(int id) {
        return Objects.requireNonNull(getTimestamp(id, true));
    }

    private static @Nullable Timestamps getTimestamp(int id, boolean createIfNoneSaved) {
        assert id > 0;
        Timestamps timestamps = ourTimestampsCache.get(id);
        if (timestamps == null) {
            TimestampsImmutable immutable = storage().readTimestamps(id);
            if (immutable == null) {
                if (createIfNoneSaved) {
                    timestamps = new Timestamps();
                }
                else {
                    return null;
                }
            }
            else {
                timestamps = immutable.toMutableTimestamps();
            }
        }
        ourTimestampsCache.cacheOrGet(id, timestamps);
        return timestamps;
    }

    @TestOnly
    public static boolean hasIndexingTimeStamp(int fileId) {
        Timestamps timestamp = getTimestamp(fileId, false);
        return timestamp != null && timestamp.hasIndexingTimeStamp();
    }

    public static void update(int fileId, ID<?, ?> indexName, long indexCreationStamp) {
        if (fileId < 0 || fileId == INVALID_FILE_ID) {
            return;
        }
        ourTimestampsPerFileLock.withWriteLock(fileId, () -> {
            Timestamps stamp = createOrGetTimeStamp(fileId);
            stamp.set(indexName, indexCreationStamp);
            return null;
        });
    }

    /**
     * Non-trivial means "up to date" or "outdated".
     * <p>
     * "unindexed" is not included.
     */
    public static List<ID<?, ?>> getNontrivialFileIndexedStates(int fileId) {
        if (fileId != INVALID_FILE_ID) {
            return ourTimestampsPerFileLock.withReadLock(fileId, () -> {
                try {
                    Timestamps stamp = createOrGetTimeStamp(fileId);
                    if (stamp.hasIndexingTimeStamp()) {
                        return List.copyOf(stamp.getIndexIds());
                    }
                }
                catch (InvalidVirtualFileAccessException ignored /*ok to ignore it here*/) {
                }
                return Collections.emptyList();
            });
        }
        return Collections.emptyList();
    }

    public static void flushCaches() {
        doFlush();
        ourFlushLock.writeLock().lock(); // wait until all doFlush in other threads are finished. TODO-ank: cooperate, not wait
        ourFlushLock.writeLock().unlock();
    }

    /** Persist cached data {@link Timestamps} for finishedFile */
    public static void flushCache(int finishedFile) {
        boolean exit = ourTimestampsPerFileLock.withReadLock(finishedFile, () -> {
            Timestamps timestamps = ourTimestampsCache.get(finishedFile);
            if (timestamps == null) {
                return true;
            }
            if (!timestamps.isDirty()) {
                ourTimestampsCache.remove(finishedFile);
                return true;
            }
            return false;
        });
        if (exit) {
            return;
        }

        while (!ourFinishedFiles.offer(finishedFile)) {
            doFlush();
        }
    }

    @TestOnly
    public static int[] dumpCachedUnfinishedFiles() {
        return ourTimestampsPerFileLock.withAllLocksWriteLocked(() -> {
            int[] cachedKeys = ourTimestampsCache
                .int2ObjectEntrySet()
                .stream()
                .filter(e -> e.getValue().isDirty())
                .mapToInt(Int2ObjectMap.Entry::getIntKey)
                .toArray();

            if (cachedKeys.length == 0) {
                return ArrayUtil.EMPTY_INT_ARRAY;
            }
            else {
                IntSet cachedIds = new IntArraySet(cachedKeys);
                Set<Integer> finishedIds = new HashSet<>(ourFinishedFiles);
                cachedIds.removeAll(finishedIds);
                return cachedIds.toIntArray();
            }
        });
    }

    private static void doFlush() {
        ourFlushLock.readLock().lock();
        try {
            List<Integer> files = new ArrayList<>(ourFinishedFiles.size());
            ourFinishedFiles.drainTo(files);

            if (!files.isEmpty()) {
                for (int fileId : files) {
                    IOException exception = ourTimestampsPerFileLock.withWriteLock(fileId, () -> {
                        try {
                            Timestamps timestamp = ourTimestampsCache.remove(fileId);
                            if (timestamp == null) {
                                return null;
                            }

                            if (timestamp.isDirty() /*&& file.isValid()*/) {
                                //RC: writeTimestamps() _could_ be re-implemented via raw attribute bytebuffer access, but now I don't see
                                //    the benefits for now: doFlush() is mostly outside the critical path, while implementing
                                //    timestamps.writeToBuffer(buffer) is complicated with all those variable-sized numbers used.
                                storage().writeTimestamps(fileId, timestamp.toImmutable());
                            }
                            return null;
                        }
                        catch (IOException e) {
                            return e;
                        }
                    });
                    if (exception != null) {
                        throw new UncheckedIOException(exception);
                    }
                }
            }
        }
        finally {
            ourFlushLock.readLock().unlock();
        }
    }

    static boolean isDirty() {
        return !ourFinishedFiles.isEmpty();
    }

    static void close() throws IOException {
        flushCaches();
        ourStorage.close();
    }
}
