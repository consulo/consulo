// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

import consulo.virtualFileSystem.internal.FSRecordsProxy;
import consulo.virtualFileSystem.internal.Unmappable;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

/**
 * Reference to FastFileAttribute (e.g. {@link consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes.IntFileAttributeAccessor}
 * or {@link consulo.virtualFileSystem.impl.internal.mapped.FastFileAttributes.Int4FileAttribute}) will be invalidated on VFS close.
 * {@link AutoRefreshingOnVfsCloseRef} tracks VFS close events and recreates references automatically after VFS re-mounted.
 */
public class AutoRefreshingOnVfsCloseRef<T extends Closeable> implements Closeable, Unmappable {
    public interface Factory<T> {
        T create(FSRecordsProxy vfs) throws IOException;
    }

    private final Factory<T> myFactory;

    private volatile @Nullable T myAttributeAccessor;

    public AutoRefreshingOnVfsCloseRef(Factory<T> factory) {
        myFactory = factory;
    }

    public T get() throws IOException {
        // we need synchronized to make sure that we don't create too many T instances from different threads.
        // attributeAccessor itself is volatile, and will be `null`-ed without synchronized (because attributeAccessor can become invalid
        // immediately after synchronized block finished, so there must be another way to make sure that initialization and shutdown
        // are not running in parallel)
        T accessor = myAttributeAccessor;
        if (accessor != null) {
            return accessor;
        }

        synchronized (this) {
            T existing = myAttributeAccessor;
            if (existing != null) {
                return existing;
            }

            FSRecordsProxy fsRecords = FSRecordsProxy.getInstance();
            T newAccessor = myFactory.create(fsRecords);

            myAttributeAccessor = newAccessor;
            fsRecords.addCloseable(this);

            return newAccessor;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            T attributeAccessor = myAttributeAccessor;
            if (attributeAccessor != null) {
                attributeAccessor.close();
            }
        }
        finally {
            myAttributeAccessor = null; //will be re-opened on next get() call
        }
    }

    @Override
    public void closeAndUnsafelyUnmap() throws IOException {
        try {
            T attributeAccessor = myAttributeAccessor;
            if (attributeAccessor instanceof Unmappable unmappable) {
                unmappable.closeAndUnsafelyUnmap();
            }
            else if (attributeAccessor != null) {
                attributeAccessor.close();
            }
        }
        finally {
            myAttributeAccessor = null;
        }
    }
}
