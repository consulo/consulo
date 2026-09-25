// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.index.io.stats.CachedChannelsStatistics;
import consulo.util.lang.SystemProperties;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Constants, params, static functions around file page caching
 */
public final class PageCacheUtils {
    public static final int CHANNELS_CACHE_CAPACITY = SystemProperties.getIntProperty("paged.file.storage.open.channel.cache.capacity", 400);

    public static final FileChannelOpener RESILIENT_CHANNEL_OPENER = (path, readOnly) -> {
        return new ResilientFileChannel(path, readOnly ? new OpenOption[]{READ} : new OpenOption[]{READ, WRITE, CREATE});
    };

    /**
     * Channels cache-bypassing accessors holder
     */
    private static final UncachedChannelsAccessors CHANNELS_NO_CACHE = new UncachedChannelsAccessors(RESILIENT_CHANNEL_OPENER);

    /**
     * Shared channels cache
     */
    private static final OpenChannelsCache CHANNELS_CACHE = new OpenChannelsCache(
        "shared-channels-cache",
        CHANNELS_CACHE_CAPACITY,
        RESILIENT_CHANNEL_OPENER
    );

    public static ChannelsAccessor getChannelsAccessor(boolean cacheChannels,
                                                       boolean readOnly) {
        return cacheChannels ? getCachedChannelsAccessor(readOnly) : getUncachedChannelsAccessor(readOnly);
    }

    public static ChannelsAccessor getCachedChannelsAccessor(boolean readOnly) {
        return readOnly ? CHANNELS_CACHE.asReadOnly() : CHANNELS_CACHE.asWritable();
    }

    public static CachedChannelsStatistics getChannelsStatistics() {
        return CHANNELS_CACHE.getStatistics().plus(CHANNELS_NO_CACHE.getStatistics());
    }

    private static ChannelsAccessor getUncachedChannelsAccessor(boolean readOnly) {
        return readOnly ? CHANNELS_NO_CACHE.asReadOnly() : CHANNELS_NO_CACHE.asWritable();
    }

    /**
     * 'Emulates' {@linkplain OpenChannelsCache} API for uniformity.
     */
    private static final class UncachedChannelsAccessors {
        private final AtomicInteger myOperationsExecuted = new AtomicInteger(0);

        private final ChannelsAccessor myReadOnlyAccessor;
        private final ChannelsAccessor myWritableAccessor;

        private UncachedChannelsAccessors(FileChannelOpener channelOpener) {
            myReadOnlyAccessor = new UncachedChannelsAccessor(/*readOnly: */true, channelOpener);
            myWritableAccessor = new UncachedChannelsAccessor(/*readOnly: */false, channelOpener);
        }

        private ChannelsAccessor asReadOnly() {
            return myReadOnlyAccessor;
        }

        private ChannelsAccessor asWritable() {
            return myWritableAccessor;
        }

        private CachedChannelsStatistics getStatistics() {
            return new CachedChannelsStatistics(0, 0, 0, /*bypassedCache: */myOperationsExecuted.get(), 0);
        }

        @Override
        public String toString() {
            return "UncachedChannelsAccessors";
        }

        private final class UncachedChannelsAccessor implements ChannelsAccessor {
            private final boolean myReadOnly;
            private final FileChannelOpener myChannelOpener;

            private UncachedChannelsAccessor(boolean readOnly,
                                             FileChannelOpener channelOpener) {
                myReadOnly = readOnly;
                myChannelOpener = channelOpener;
            }

            @Override
            public boolean isReadOnly() {
                return myReadOnly;
            }

            @Override
            public <T> T executeOp(Path path,
                                   FileChannelOperation<T> operation) throws IOException {
                myOperationsExecuted.incrementAndGet();
                try (OpenChannelsCache.ChannelDescriptor desc = new OpenChannelsCache.ChannelDescriptor(path, myReadOnly, myChannelOpener)) {
                    return operation.execute(desc.channel());
                }
            }

            @Override
            public <T> T executeIdempotentOp(Path path,
                                             FileChannelInterruptsRetryer.FileChannelIdempotentOperation<T> operation)
                throws IOException {
                myOperationsExecuted.incrementAndGet();
                try (OpenChannelsCache.ChannelDescriptor desc = new OpenChannelsCache.ChannelDescriptor(path, myReadOnly, myChannelOpener)) {
                    return desc.executeIdempotentOp(operation);
                }
            }

            @Override
            public void closeChannel(Path path) {
            }

            @Override
            public String toString() {
                return "UncachedChannelsAccessor[readOnly=" + myReadOnly + ']';
            }
        }
    }

    private PageCacheUtils() {
        throw new AssertionError("Not for instantiation");
    }
}
