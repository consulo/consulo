// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.language.index.impl.internal.dependencies.AppIndexingDependenciesService;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.dependencies.IsFileChangedResult;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.perFileVersion.LongFileAttribute;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * An object dedicated to manage persistent {@code isIndexed} file flag.
 * <p>
 * For each file it saves the file mod count and {@link AppIndexingDependenciesService#getCurrent} at the moment when the file was indexed.
 * The IndexingFlag is used to quickly check if all indexes for a file are up to date (see IJPL-229).
 * But if IndexingFlag is not up to date, it doesn't necessarily mean that indexes for a file are outdated.
 * <p>
 * IndexingFlag can also be used to check the dirty files from the previous IDE sessions for which {@link IndexingStamp}
 * may not have been updated or the change was not persisted to disk.
 * <p>
 * The alternative is {@link IndexingStamp} which contains information per-index but can become outdated (see the doc) and is slower.
 * So in practice the combination of the two should be used (see {@link FileBasedIndexImpl#getIndexingState}).
 */
public final class IndexingFlag {
    private static final FileAttribute ATTRIBUTE = new FileAttribute("indexing.flag", 1, true);

    /** fileId -> ({@link FileIndexingStamp} as int64) */
    private static final LongFileAttribute ourPersistence = LongFileAttribute.overFastAttribute(ATTRIBUTE);
    private static final StripedIndexingStampLock ourHashes = new StripedIndexingStampLock();

    public static final long NON_EXISTENT_HASH = StripedIndexingStampLock.NON_EXISTENT_HASH;

    public static long getNonExistentHash() {
        return NON_EXISTENT_HASH;
    }

    public static void cleanupProcessedFlag(String debugReason) {
        AppIndexingDependenciesService.getInstance().invalidateAllStamps(debugReason);
    }

    private static @Nullable VirtualFileWithId asApplicable(VirtualFile file) {
        return file instanceof VirtualFileWithId fileWithId ? fileWithId : null;
    }

    public static void cleanProcessedFlagRecursively(VirtualFile file) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return;
        }
        cleanProcessingFlag(file);
        if (!(fileWithId instanceof NewVirtualFile newVirtualFile)) {
            return;
        }
        if (!newVirtualFile.isDirectory()) {
            return;
        }
        for (VirtualFile child : newVirtualFile.getCachedChildren()) {
            //TODO RC: shouldn't we use .iterInDbChildren()? Because it could be the child was loaded at some point, left it's
            //         mark in IndexingFlag, but then the parent GCed from the VFS cache, and reloaded later, with fewer
            //         children in-memory
            cleanProcessedFlagRecursively(child);
        }
    }

    public static void cleanProcessingFlag(VirtualFile file) {
        setFileIndexed(file, ProjectIndexingDependenciesService.NULL_STAMP);
    }

    public static void cleanProcessingFlag(int fileId) {
        // the file might have already been deleted, so there might be no VirtualFile for given fileId
        //TODO RC: sometimes incorrect fileIds (>maxAllocatedFileId) are coming here. Probably, it is not that incorrect to
        //         clean incorrect fileId? Maybe we should just ignore incorrect fileId (because they are effectively already
        //         'cleaned' in some sense) instead of throwing an exception?
        setFileIndexed(fileId, ProjectIndexingDependenciesService.NULL_STAMP);
    }

    public static void setFileIndexed(VirtualFile file, FileIndexingStamp stamp) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return;
        }
        setFileIndexed(fileWithId.getId(), stamp);
    }

    private static void setFileIndexed(int fileId, FileIndexingStamp stamp) {
        stamp.store(s -> {
            try {
                ourPersistence.writeLong(fileId, s);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static boolean isFileIndexed(VirtualFile file, FileIndexingStamp stamp) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return false;
        }
        return stamp.isSame(readLong(fileWithId.getId()));
    }

    /**
     * Possible situations:
     * <ul>
     * <li>Regular situation when we can trust {@link IndexingStamp} which tells us if any given index is up to date for any given file.
     *   {@link IsFileChangedResult#UNKNOWN} is returned and then later in {@link FileBasedIndexImpl#getIndexingState}
     *   the actual {@link IndexingStamp} is checked.</li>
     *
     * <li>Situation when we cannot trust {@link IndexingStamp}.
     *   This situation occurs if we lost the whole list of dirty files from the previous session.
     *   In this case the result from {@code stamp} ({@link IsFileChangedResult#YES} or {@link IsFileChangedResult#NO}) is returned.
     *   I.e., the file will be either considered fully indexed if file mod count AND IDE configuration didn't change.
     *   Or it'll be considered fully unindexed if the file OR IDE configuration is changed.
     *   It also means that if IDE configuration changed, and we lost the list of dirty files, then we'll re-index all the files.</li>
     * </ul>
     */
    public static IsFileChangedResult isFileChanged(VirtualFile file, FileIndexingStamp stamp) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return IsFileChangedResult.UNKNOWN;
        }
        return stamp.isFileChanged(readLong(fileWithId.getId()));
    }

    public static long getOrCreateHash(VirtualFile file) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return NON_EXISTENT_HASH;
        }
        return ourHashes.getHash(fileWithId.getId());
    }

    public static void unlockFile(VirtualFile file) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return;
        }
        ourHashes.releaseHash(fileWithId.getId());
    }

    public static void setIndexedIfFileWithSameLock(VirtualFile file, long lockObject, FileIndexingStamp stamp) {
        VirtualFileWithId fileWithId = asApplicable(file);
        if (fileWithId == null) {
            return;
        }
        long hash = ourHashes.releaseHash(fileWithId.getId());
        if (isFileIndexed(file, stamp)) {
            return;
        }

        if (hash == lockObject) {
            setFileIndexed(file, stamp);
        }
        else {
            cleanProcessingFlag(file);
        }
    }

    public static void unlockAllFiles() {
        ourHashes.clear();
    }

    public static void reloadAttributes() {
        closePersistence();//will be reopened on next access
    }

    public static void close() {
        unlockAllFiles();
        closePersistence();
    }

    public static int[] dumpLockedFiles() {
        return ourHashes.dumpIds();
    }

    private static long readLong(int fileId) {
        try {
            return ourPersistence.readLong(fileId);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void closePersistence() {
        try {
            ourPersistence.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private IndexingFlag() {
    }
}
