// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.index.io;

import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Context of storage operations: which file page cache to use, which kind of locking to use, how to cache file
 * channels...
 * <p>
 * RC: 'lock' part historically was important, but is less important now, since for new FilePageCache locking is
 * decided per storage. Probably class should be renamed to just StorageContext at some moment.
 */
public final class StorageLockContext {
    private final boolean myCheckThreadAccess;
    private final ReentrantLock myLock;
    private final PagedFileStorage.StorageLock myStorageLock;

    private final ChannelsAccessor myReadOnlyChannelsAccessor;
    private final ChannelsAccessor myWritableChannelsAccessor;

    StorageLockContext(PagedFileStorage.StorageLock lock,
                       boolean checkAccess,
                       ChannelsAccessor readOnlyChannelsAccessor,
                       ChannelsAccessor writableChannelsAccessor) {
        myLock = new ReentrantLock();
        myStorageLock = lock;
        myCheckThreadAccess = checkAccess;

        if (!readOnlyChannelsAccessor.isReadOnly()) {
            throw new IllegalArgumentException("readOnlyAccessor must be read-only: " + readOnlyChannelsAccessor);
        }
        if (writableChannelsAccessor.isReadOnly()) {
            throw new IllegalArgumentException("writableAccessor must be writable: " + writableChannelsAccessor);
        }
        myReadOnlyChannelsAccessor = readOnlyChannelsAccessor;
        myWritableChannelsAccessor = writableChannelsAccessor;
    }

    StorageLockContext(PagedFileStorage.StorageLock lock,
                       boolean checkAccess,
                       boolean cacheChannels) {
        this(lock,
            checkAccess,
            PageCacheUtils.getChannelsAccessor(cacheChannels, /*readOnly: */true),
            PageCacheUtils.getChannelsAccessor(cacheChannels, /*readOnly: */false)
        );
    }

    public StorageLockContext(boolean checkAccess,
                              ChannelsAccessor readOnlyChannelsAccessor,
                              ChannelsAccessor writableChannelsAccessor) {
        this(PagedFileStorage.ourLock, checkAccess, readOnlyChannelsAccessor, writableChannelsAccessor);
    }

    public StorageLockContext(boolean checkAccess,
                              boolean cacheChannels) {
        this(PagedFileStorage.ourLock, checkAccess, cacheChannels);
    }

    public StorageLockContext(boolean checkAccess) {
        this(checkAccess, false);
    }

    public StorageLockContext() {
        this(false, false);
    }

    public void lock() {
        myLock.lock();
    }

    public void unlock() {
        myLock.unlock();
    }

    PagedFileStorage.StorageLock getStorageLock() {
        return myStorageLock;
    }

    void checkThreadAccess() {
        if (myCheckThreadAccess && !myLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Must hold StorageLock lock to access PagedFileStorage");
        }
    }

    public ChannelsAccessor getChannelsAccessor(boolean readOnly) {
        return readOnly ? myReadOnlyChannelsAccessor : myWritableChannelsAccessor;
    }

    @Override
    public String toString() {
        return "StorageLockContext[" +
            "checkThreadAccess: " + myCheckThreadAccess +
            ", channels: " + myReadOnlyChannelsAccessor + "/" + myWritableChannelsAccessor +
            ']';
    }

    /**
     * Checks that no cached channel remains for the file in either read-only or writable accessor.
     */
    public void assertNoOpenChannels(Path path) {
        StringBuilder openChannels = new StringBuilder();
        appendOpenChannelDescription(openChannels, "read-only", myReadOnlyChannelsAccessor, path);
        appendOpenChannelDescription(openChannels, "writable", myWritableChannelsAccessor, path);
        if (openChannels.length() > 0) {
            throw new AssertionError("Open channels remain for " + path + ":\n" + openChannels);
        }
    }

    private static void appendOpenChannelDescription(StringBuilder openChannels,
                                                     String accessorMode,
                                                     ChannelsAccessor channelsAccessor,
                                                     Path path) {
        if (channelsAccessor instanceof DiagnosticChannelsAccessor diagnosticChannelsAccessor) {
            String description = diagnosticChannelsAccessor.describeCachedChannelOrNull(path);
            if (description != null) {
                openChannels.append(accessorMode).append(" accessor: ").append(description).append('\n');
            }
        }
    }
}
