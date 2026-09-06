// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes;
import consulo.virtualFileSystem.internal.Unmappable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This is a simple wrapper around {@link SpecializedFileAttributes}, either fast or a regular one.
 * The main function of this wrapper is to make the attribute auto-reopenable, with help of {@link AutoRefreshingOnVfsCloseRef}:
 * which means that the storage will be automatically reset if VFS is rebuilt, and also automatically reopened, if {@link #close}-ed
 *
 * @see AutoRefreshingOnVfsCloseRef
 */
public interface LongFileAttribute extends Closeable, Unmappable {
    static boolean shouldUseFastAttributes() {
        return true;
    }

    static LongFileAttribute create(String id, int version) {
        boolean fast = shouldUseFastAttributes();
        String suffix = fast ? ".fast" : "";
        FileAttribute attribute = new FileAttribute(id + suffix, version, true);
        return fast ? overFastAttribute(attribute) : overRegularAttribute(attribute);
    }

    static LongFileAttribute overRegularAttribute(FileAttribute attribute) {
        return new LongFileAttributeImpl(attribute, null);
    }

    /**
     * By default, file will be created in "ContainerPathManager.getIndexRoot()/fastAttributes/attribute.id"
     */
    static LongFileAttribute overFastAttribute(FileAttribute attribute) {
        Path attributesFilePath = ContainerPathManager.get().getIndexRoot().toPath().resolve("fastAttributes").resolve(attribute.getId());
        return overFastAttribute(attribute, attributesFilePath);
    }

    static LongFileAttribute overFastAttribute(FileAttribute attribute, Path path) {
        Logger.getInstance(LongFileAttribute.class).assertTrue(attribute.isFixedSize(), "Should be fixed size: " + attribute);
        return new LongFileAttributeImpl(attribute, path);
    }

    long readLong(int fileId) throws IOException;

    void writeLong(int fileId, long value) throws IOException;
}
