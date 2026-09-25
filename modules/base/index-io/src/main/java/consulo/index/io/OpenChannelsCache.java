// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.index.io.FileChannelInterruptsRetryer.FileChannelIdempotentOperation;
import consulo.index.io.stats.CachedChannelsStatistics;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cache of opened {@link FileChannel}s.
 * Cache eviction policy is kind of LRU -- the oldest channel accessed is the first to be evicted, given it is not used right now.
 * <p>
 * The cache exposes two mode-bound {@link ChannelsAccessor} views: {@link #asReadOnly()} and {@link #asWritable()}.
 * BEWARE: cache caches (potentially) 2 different {@linkplain FileChannel} instances: readOnly and !readOnly.
 * Generally, it is not guaranteed these 2 different FileChannels instances always share the same data -- they
 * could, but also there could be some temporary difference in the content visible via readOnly and !readOnly
 * FileChannel. So, better avoid accessing the same path via 2 different readOnly/!readOnly FileChannels: use
 * the single accessor for _all_ the accesses to the given Path.
 */
public final class OpenChannelsCache {
    /**
     * for {@linkplain #toString()}
     */
    private final String myCacheName;

    /**
     * Max channels to keep open in cache
     */
    private final int myCapacity;

    //@GuardedBy("cacheLock")
    private final Map<CacheKey, ChannelDescriptor> myCachedChannels;
    //@GuardedBy("cacheLock")
    private final Map<CacheKey, Thread> myOpeningChannels = new HashMap<>();
    //@GuardedBy("cacheLock")
    private final Map<CacheKey, Thread> myClosingChannels = new HashMap<>();

    private final transient Object myCacheLock = new Object();

    private final FileChannelOpener myChannelOpener;

    private final transient ChannelsAccessor myReadOnlyAccessor;
    private final transient ChannelsAccessor myWritableAccessor;


    //statistics of the caching efficacy:
    private final PerModeStatistics myReadOnlyStats = new PerModeStatistics();
    private final PerModeStatistics myWritableStats = new PerModeStatistics();


    /**
     * @param cacheName just for debugging
     */
    public OpenChannelsCache(String cacheName,
                             int capacity,
                             FileChannelOpener channelOpener) {
        myCacheName = cacheName;
        myCapacity = capacity;
        myCachedChannels = new LinkedHashMap<>(capacity, 0.5f, /*orderByAccess: */true);
        myChannelOpener = channelOpener;
        myReadOnlyAccessor = new AccessorView(/*readOnly: */true);
        myWritableAccessor = new AccessorView(/*readOnly: */false);
    }

    public ChannelsAccessor asReadOnly() {
        return myReadOnlyAccessor;
    }

    public ChannelsAccessor asWritable() {
        return myWritableAccessor;
    }

    public CachedChannelsStatistics getStatistics() {
        synchronized (myCacheLock) {
            return new CachedChannelsStatistics(
                myReadOnlyStats.myHitCount + myWritableStats.myHitCount,
                myReadOnlyStats.myMissCount + myWritableStats.myMissCount,
                myReadOnlyStats.myLoadCount + myWritableStats.myLoadCount,
                /*bypassedCache: */0,
                myCapacity
            );
        }
    }

    @Override
    public String toString() {
        return "OpenChannelsCache[" + myCacheName + "]" +
            "[capacity: " + myCapacity + ", cached: " + myCachedChannels.size() + ", opener: " + myChannelOpener + "]";
    }

    /**
     * Note: this implementation supplies {@link ResilientFileChannel} to processor. {@link ResilientFileChannel}
     * is a FileChannel implementation that tries to ensure each FileChannel operation is completed,
     * or not started at all, but not interrupted in the middle. If something interrupts 'elementary'
     * FileChannel ops, like read/write -- those ops are retried, invisibly for processor -- see class
     * description for details. But it comes with small performance cost, and also the {@link ResilientFileChannel}
     * does not implement some FileChannel operations, so be aware.
     */
    private <T> T executeOp(Path path,
                            FileChannelOperation<T> operation,
                            boolean readOnly) throws IOException {
        ChannelDescriptor descriptor = acquireDescriptor(path, readOnly);
        //channel access is NOT guarded by the cacheLock
        try {
            return operation.execute(descriptor.channel());
        }
        finally {
            releaseDescriptor(descriptor);
        }
    }

    /**
     * Parameter {@param operation} should be idempotent because sometimes calculation might be restarted
     * when the file channel was closed by thread interruption
     */
    private <T> T executeIdempotentOp(Path path,
                                      FileChannelIdempotentOperation<T> operation,
                                      boolean readOnly) throws IOException {
        ChannelDescriptor descriptor = acquireDescriptor(path, readOnly);
        //channel access is NOT guarded by the cacheLock
        try {
            return descriptor.executeIdempotentOp(operation);
        }
        finally {
            releaseDescriptor(descriptor);
        }
    }

    private ChannelDescriptor acquireDescriptor(Path path,
                                                boolean readOnly) throws IOException {
        CacheKey key = new CacheKey(path, readOnly);
        boolean descriptorsWereDropped = false;
        boolean cacheWasOverCapacity = false;
        while (true) {
            List<DetachedChannelDescriptor> descriptorsToClose;
            synchronized (myCacheLock) {
                waitForPendingOpen(key);
                waitForPendingClose(key);

                ChannelDescriptor descriptor = myCachedChannels.get(key);
                if (descriptor != null) {
                    PerModeStatistics statistics = statisticsFor(readOnly);
                    statistics.myHitCount++;
                    descriptor.lock();
                    return descriptor;
                }

                EvictionResult eviction = detachOverCachedChannels(1);
                descriptorsToClose = eviction.myDescriptorsToClose;
                cacheWasOverCapacity |= eviction.myCacheWasOverCapacity;
                if (descriptorsToClose.isEmpty()) {
                    myOpeningChannels.put(key, Thread.currentThread());
                    break;
                }

                descriptorsWereDropped = true;
            }

            closeDetachedChannels(descriptorsToClose);
        }

        ChannelDescriptor descriptor = null;
        try {
            descriptor = new ChannelDescriptor(path, readOnly, myChannelOpener);

            while (true) {
                List<DetachedChannelDescriptor> descriptorsToClose;
                synchronized (myCacheLock) {
                    EvictionResult eviction = detachOverCachedChannels(1);
                    descriptorsToClose = eviction.myDescriptorsToClose;
                    cacheWasOverCapacity |= eviction.myCacheWasOverCapacity;
                    if (descriptorsToClose.isEmpty()) {
                        myCachedChannels.put(key, descriptor);
                        PerModeStatistics statistics = statisticsFor(readOnly);
                        if (descriptorsWereDropped || cacheWasOverCapacity) {
                            statistics.myMissCount++;
                        }
                        else {
                            statistics.myLoadCount++;
                        }
                        descriptor.lock();
                        finishOpeningUnderLock(key);
                        return descriptor;
                    }

                    descriptorsWereDropped = true;
                }

                closeDetachedChannels(descriptorsToClose);
            }
        }
        catch (Throwable t) {
            finishOpening(key);
            if (descriptor != null) {
                try {
                    descriptor.close();
                }
                catch (Throwable closeError) {
                    t.addSuppressed(closeError);
                }
            }
            throwAsIOExceptionOrUnchecked(t);
            throw new AssertionError("unreachable");
        }
    }

    // If there is a channel for a key pending to open -- waits for it to be actually opened.
    // Must be called under cacheLock (but releases the lock while waiting).
    private void waitForPendingOpen(CacheKey key) {
        waitForPendingOperation(key, myOpeningChannels);
    }

    // If there is a channel for a key pending to close -- waits for it to be actually closed.
    // Must be called under cacheLock (but releases the lock while waiting).
    private void waitForPendingClose(CacheKey key) {
        waitForPendingOperation(key, myClosingChannels);
    }

    private void waitForPendingOperation(CacheKey key,
                                         Map<CacheKey, Thread> pendingOperationThreads) {
        boolean interrupted = false;
        while (true) {
            Thread thread = pendingOperationThreads.get(key);
            if (thread == null || thread == Thread.currentThread()) {
                break;
            }
            try {
                myCacheLock.wait();
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private PerModeStatistics statisticsFor(boolean readOnly) {
        return readOnly ? myReadOnlyStats : myWritableStats;
    }

    private void releaseDescriptor(ChannelDescriptor descriptor) {
        synchronized (myCacheLock) {
            descriptor.unlock();
        }
    }

    private void closeChannel(Path path,
                              boolean readOnly) throws IOException {
        DetachedChannelDescriptor descriptorToClose;
        synchronized (myCacheLock) {
            CacheKey key = new CacheKey(path, readOnly);
            waitForPendingOpen(key);
            waitForPendingClose(key);
            descriptorToClose = detachChannel(key);
        }

        if (descriptorToClose != null) {
            closeDetachedChannel(descriptorToClose);
        }
    }

    /// @param slotsToFree after this method it should be at least slotsToFree slots available until capacity
    //@GuardedBy(cacheLock)
    @SuppressWarnings("SameParameterValue")
    private EvictionResult detachOverCachedChannels(int slotsToFree) {
        int channelsToEvict = myCachedChannels.size() - myCapacity + slotsToFree;

        if (channelsToEvict <= 0) {
            return EvictionResult.NOT_NEEDED;
        }

        List<CacheKey> keysToEvict = new ArrayList<>();
        for (Map.Entry<CacheKey, ChannelDescriptor> entry : myCachedChannels.entrySet()) {
            if (channelsToEvict <= 0) {
                break;
            }
            ChannelDescriptor channelDescriptor = entry.getValue();
            if (!channelDescriptor.isLocked()) {
                keysToEvict.add(entry.getKey());
                channelsToEvict--;
            }
        }

        List<DetachedChannelDescriptor> descriptorsToClose = new ArrayList<>(keysToEvict.size());
        for (CacheKey keyToDrop : keysToEvict) {
            DetachedChannelDescriptor descriptorToClose = detachChannel(keyToDrop);
            if (descriptorToClose != null) {
                descriptorsToClose.add(descriptorToClose);
            }
        }

        return new EvictionResult(/* wasOverCapacity: */ true, descriptorsToClose);
    }

    //@GuardedBy(cacheLock)
    private @Nullable DetachedChannelDescriptor detachChannel(CacheKey key) {
        ChannelDescriptor descriptor = myCachedChannels.remove(key);

        if (descriptor != null) {
            assert !descriptor.isLocked() : "Channel is in use: " + descriptor;
            myClosingChannels.put(key, Thread.currentThread());
            return new DetachedChannelDescriptor(key, descriptor);
        }

        return null;
    }

    private void closeDetachedChannels(List<DetachedChannelDescriptor> descriptorsToClose) throws IOException {
        Throwable error = null;
        for (DetachedChannelDescriptor descriptorToClose : descriptorsToClose) {
            try {
                closeDetachedChannel(descriptorToClose);
            }
            catch (Throwable t) {
                if (error == null) {
                    error = t;
                }
                else {
                    error.addSuppressed(t);
                }
            }
        }

        if (error == null) {
            return;
        }
        throwAsIOExceptionOrUnchecked(error);
    }

    private static void throwAsIOExceptionOrUnchecked(Throwable error) throws IOException {
        if (error instanceof IOException ioException) {
            throw ioException;
        }
        if (error instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (error instanceof Error e) {
            throw e;
        }
        throw new IOException(error);
    }

    private void closeDetachedChannel(DetachedChannelDescriptor descriptorToClose) throws IOException {
        try {
            descriptorToClose.myDescriptor.close();
        }
        finally {
            finishClosing(descriptorToClose.myKey);
        }
    }

    private void finishClosing(CacheKey key) {
        synchronized (myCacheLock) {
            myClosingChannels.remove(key);
            myCacheLock.notifyAll();
        }
    }

    private void finishOpening(CacheKey key) {
        synchronized (myCacheLock) {
            finishOpeningUnderLock(key);
        }
    }

    //@GuardedBy(cacheLock)
    private void finishOpeningUnderLock(CacheKey key) {
        myOpeningChannels.remove(key);
        myCacheLock.notifyAll();
    }

    static final class ChannelDescriptor implements Closeable {
        private final FileChannel myChannel;
        private final boolean myReadOnly;

        private int myLockCount = 0;

        ChannelDescriptor(Path path,
                          boolean readOnly,
                          FileChannelOpener channelOpener) throws IOException {
            myReadOnly = readOnly;
            if (!readOnly) {
                Path parent = path.getParent();
                boolean parentExists = Files.exists(parent);
                if (!parentExists) {
                    Files.createDirectories(parent);
                }
            }

            myChannel = Objects.requireNonNull(FileUtil.doIOOperation(isLastAttempt -> {
                try {
                    return channelOpener.open(path, readOnly);
                }
                catch (NoSuchFileException ex) {
                    if (!isLastAttempt) {
                        return null;
                    }

                    //provide more diagnostic info:
                    Path parent = path.getParent();
                    boolean parentExists = Files.exists(parent);

                    NoSuchFileException exception = new NoSuchFileException(
                        path.toString(), /*other: */ null,
                        "[" + path + "][readOnly: " + readOnly + "]: file doesn't exist, " +
                            "parent [" + parent + "] " + (parentExists ? "does exist" : "doesn't exist")
                    );
                    exception.addSuppressed(ex);
                    throw exception;
                }
            }));

            if (!(myChannel instanceof Resilient)) {
                throw new AssertionError("channel must be instanceof Resilient, but " + myChannel.getClass());
            }
        }

        private void lock() {
            myLockCount++;
        }

        private void unlock() {
            myLockCount--;
        }

        private boolean isLocked() {
            return myLockCount != 0;
        }

        FileChannel channel() {
            return myChannel;
        }

        <R> R executeIdempotentOp(FileChannelIdempotentOperation<R> operation) throws IOException {
            return ((Resilient) myChannel).executeOperation(operation);
        }

        @Override
        public void close() throws IOException {
            myChannel.close();
        }

        @Override
        public String toString() {
            return "ChannelDescriptor{" +
                "locks=" + myLockCount +
                ", channel=" + myChannel +
                ", readOnly=" + myReadOnly +
                '}';
        }
    }

    private final class AccessorView implements ChannelsAccessor, DiagnosticChannelsAccessor {
        private final boolean myReadOnly;

        private AccessorView(boolean readOnly) {
            myReadOnly = readOnly;
        }

        @Override
        public boolean isReadOnly() {
            return myReadOnly;
        }

        @Override
        public <T> T executeOp(Path path,
                               FileChannelOperation<T> operation) throws IOException {
            return OpenChannelsCache.this.executeOp(path, operation, myReadOnly);
        }

        @Override
        public <T> T executeIdempotentOp(Path path,
                                         FileChannelIdempotentOperation<T> operation) throws IOException {
            return OpenChannelsCache.this.executeIdempotentOp(path, operation, myReadOnly);
        }

        @Override
        public void closeChannel(Path path) throws IOException {
            OpenChannelsCache.this.closeChannel(path, myReadOnly);
        }

        @Override
        public @Nullable String describeCachedChannelOrNull(Path path) {
            synchronized (myCacheLock) {
                ChannelDescriptor descriptor = myCachedChannels.get(new CacheKey(path, myReadOnly));
                return descriptor == null ? null : descriptor.toString();
            }
        }

        @Override
        public String toString() {
            return "OpenChannelsCache[" + myCacheName + "].AccessorView[readOnly: " + myReadOnly + ']';
        }
    }

    private static final class PerModeStatistics {
        private int myHitCount;
        private int myMissCount;
        private int myLoadCount;
    }

    private static final class EvictionResult {
        public static final EvictionResult NOT_NEEDED = new EvictionResult(/* overCapacity: */ false, Collections.emptyList());

        private final boolean myCacheWasOverCapacity;
        private final List<DetachedChannelDescriptor> myDescriptorsToClose;

        private EvictionResult(boolean cacheWasOverCapacity,
                               List<DetachedChannelDescriptor> descriptorsToClose) {
            myCacheWasOverCapacity = cacheWasOverCapacity;
            myDescriptorsToClose = descriptorsToClose;
        }
    }

    private static final class DetachedChannelDescriptor {
        private final CacheKey myKey;
        private final ChannelDescriptor myDescriptor;

        private DetachedChannelDescriptor(CacheKey key,
                                          ChannelDescriptor descriptor) {
            myKey = key;
            myDescriptor = descriptor;
        }
    }

    private static final class CacheKey {
        private final Path myPath;
        private final boolean myReadOnly;

        private CacheKey(Path path,
                         boolean readOnly) {
            myPath = path;
            myReadOnly = readOnly;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey key)) {
                return false;
            }
            return myReadOnly == key.myReadOnly && myPath.equals(key.myPath);
        }

        @Override
        public int hashCode() {
            return myPath.hashCode() * 31 + (myReadOnly ? 1 : 0);
        }
    }
}
