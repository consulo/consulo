// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

import consulo.index.io.CachingEnumerator;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.PersistentEnumeratorBase;
import consulo.index.io.data.DataEnumerator;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import consulo.virtualFileSystem.internal.Unmappable;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A class that allows associating enumerable objects and virtual files.
 * Physical storage contains two pieces: {@link PersistentEnumeratorBase} and {@link IntFileAttribute}.
 * Physical storage ({@code exclusiveDir} directory with all its content) will be deleted on VFS rebuild.
 *
 * The main problems that this class is intended to solve:
 * 1. store files used for enumerated data ({@link PersistentEnumeratorBase}) and {@link IntFileAttribute} in the same folder
 * 2. delete all the data on VFS rebuild
 *
 * <p>
 * {@code exclusiveDir} is a directory to use for storage. {@link EnumeratedFastFileAttribute} assumes that it
 * uses this directory exclusively, i.e. no other classes keep their files there. {@link EnumeratedFastFileAttribute} will
 * delete the whole directory on VFS rebuild. All the data is kept in this folder only, i.e. it is enough to
 * delete it (while {@link EnumeratedFastFileAttribute} is closed) to drop all the existing data.
 * <p>
 * {@code descriptorForCache} enables caching via {@link CachingEnumerator} (or use no-cache if {@code null})
 * <p>
 * {@code createEnumerator} is a lambda that creates enumerator in specified {@code enumeratorPath}. Invoked at most once
 * (exactly once if constructor completes normally).
 */
public class EnumeratedFastFileAttribute<T> implements Closeable, Unmappable {
    public interface EnumeratorFactory<T> {
        PersistentEnumeratorBase<T> create(Path enumeratorPath) throws IOException;
    }

    private static final Logger LOG = Logger.getInstance(EnumeratedFastFileAttribute.class);

    private final Path myExclusiveDir;

    private final DataEnumerator<T> myBaseEnumerator;
    private final IntFileAttribute myBaseAttribute;
    private final List<Closeable> myToClose;

    public EnumeratedFastFileAttribute(
        Path exclusiveDir,
        FileAttribute fileAttribute,
        @Nullable KeyDescriptor<T> descriptorForCache,
        EnumeratorFactory<T> createEnumerator
    ) throws IOException {
        this(exclusiveDir, fileAttribute, descriptorForCache, FSRecordsProxy.getInstance().getCreationTimestamp(), createEnumerator);
    }

    public EnumeratedFastFileAttribute(
        Path exclusiveDir,
        FileAttribute fileAttribute,
        @Nullable KeyDescriptor<T> descriptorForCache,
        long expectedVfsCreationTimestamp,
        EnumeratorFactory<T> createEnumerator
    ) throws IOException {
        myExclusiveDir = exclusiveDir;

        VfsCreationStampChecker vfsChecker = new VfsCreationStampChecker(getVfsCreationTimestampFile());
        assert !Files.isRegularFile(exclusiveDir) : exclusiveDir + " should be a directory, or non-existing path";

        vfsChecker.runIfVfsCreationStampMismatch(expectedVfsCreationTimestamp, this::deleteStorageDir);

        PersistentEnumeratorBase<T> enumerator;
        IntFileAttribute attribute;
        try {
            enumerator = openEnumerator(createEnumerator);
            attribute = openAttribute(enumerator, fileAttribute);
        }
        catch (IOException ioe) {
            deleteStorageDir(ioe.toString());
            enumerator = openEnumerator(createEnumerator);
            attribute = openAttribute(enumerator, fileAttribute);
        }

        myToClose = List.of(enumerator, attribute);

        //TODO RC: CachingEnumerator seems to be quite an overkill on top of the enumerator!
        myBaseEnumerator = descriptorForCache == null ? enumerator : new CachingEnumerator<>(enumerator, descriptorForCache);
        myBaseAttribute = attribute;
        vfsChecker.createVfsTimestampMarkerFileIfAbsent(expectedVfsCreationTimestamp);
    }

    private PersistentEnumeratorBase<T> openEnumerator(EnumeratorFactory<T> createEnumerator) throws IOException {
        Files.createDirectories(myExclusiveDir);
        return createEnumerator.create(getEnumeratorFile());
    }

    private IntFileAttribute openAttribute(PersistentEnumeratorBase<T> enumerator, FileAttribute fileAttribute) throws IOException {
        try {
            return IntFileAttribute.overFastAttribute(fileAttribute, getAttributesFile());
        }
        catch (Throwable e) {
            enumerator.close();
            throw e;
        }
    }

    private Path getAttributesFile() {
        return myExclusiveDir.resolve("attributes");
    }

    private Path getEnumeratorFile() {
        return myExclusiveDir.resolve("enumerator");
    }

    private Path getVfsCreationTimestampFile() {
        return myExclusiveDir.resolve("vfs.stamp");
    }

    private void deleteStorageDir(String cleanupReason) {
        LOG.info("Clear " + myExclusiveDir + ". Reason: " + cleanupReason);
        if (Files.exists(myExclusiveDir)) {
            FileUtil.deleteWithRenaming(myExclusiveDir.toFile());
        }
    }

    public @Nullable T readEnumerated(int fileId) throws IOException {
        int enumValue = myBaseAttribute.readInt(fileId);
        return enumValue > 0 ? myBaseEnumerator.valueOf(enumValue) : null;
    }

    public void writeEnumerated(int fileId, T value) throws IOException {
        int enumValue = myBaseEnumerator.enumerate(value);
        myBaseAttribute.writeInt(fileId, enumValue);
    }

    public void clearValue(int fileId) throws IOException {
        myBaseAttribute.writeInt(fileId, 0);
    }

    @Override
    public void close() throws IOException {
        for (Closeable closeable : myToClose) {
            closeable.close();
        }
    }

    @Override
    public void closeAndUnsafelyUnmap() throws IOException {
        for (Closeable closeable : myToClose) {
            if (closeable instanceof Unmappable unmappable) {
                unmappable.closeAndUnsafelyUnmap();
            }
            else {
                closeable.close();
            }
        }
    }
}
