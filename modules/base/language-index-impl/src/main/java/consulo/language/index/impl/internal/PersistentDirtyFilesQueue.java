// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class PersistentDirtyFilesQueue {
    private static final Logger LOG = Logger.getInstance(PersistentDirtyFilesQueue.class);

    public static final String QUEUES_DIR_NAME = "dirty-file-queues";

    private static final long CURRENT_VERSION = 2L;

    private PersistentDirtyFilesQueue() {
    }

    private static boolean isUnittestMode() {
        Application application = ApplicationManager.getApplication();
        return application == null || application.isUnitTestMode();
    }

    public static Path getQueuesDir() {
        return ContainerPathManager.get().getIndexRoot().toPath().resolve(QUEUES_DIR_NAME);
    }

    public static Path getQueueFile() {
        return ContainerPathManager.get().getIndexRoot().toPath().resolve("dirty-file-ids");
    }

    public static Path getQueueFile(Project project) {
        return getQueuesDir().resolve(project.getLocationHash());
    }

    public static ProjectDirtyFilesQueue readProjectDirtyFilesQueue(Path queueFile, @Nullable Long currentVfsVersion) {
        IndexingQueueReadResult result = readIndexingQueue(queueFile, currentVfsVersion);
        return new ProjectDirtyFilesQueue(result.fileIds(), result.index() == null ? 0L : result.index());
    }

    public static Pair<OrphanDirtyFilesQueue, @Nullable OrphanDirtyFilesQueueDiscardReason> readOrphanDirtyFilesQueue(
        Path queueFile,
        @Nullable Long currentVfsVersion
    ) {
        IndexingQueueReadResult result = readIndexingQueue(queueFile, currentVfsVersion);
        long index = result.index() == null ? result.fileIds().size() : result.index();
        return Pair.create(new OrphanDirtyFilesQueue(result.fileIds(), index), result.orphanQueueDiscardReason());
    }

    public record IndexingQueueReadResult(
        IntList fileIds,
        @Nullable Long index,
        @Nullable OrphanDirtyFilesQueueDiscardReason orphanQueueDiscardReason
    ) {
    }

    /**
     * Project dirty files queue and orphan dirty files queue have the same format
     * Project queue: [version, vfs version, last seen index in orphan queue, ids...]
     * Orphan queue: [version, vfs version, last index in queue, ids...]
     */
    public static IndexingQueueReadResult readIndexingQueue(Path queueFile, @Nullable Long currentVfsVersion) {
        Throwable error;
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(queueFile)))) {
            IntList fileIds = new IntArrayList();
            long version = stream.readLong();
            long storedVfsVersion;
            Long index;
            if (version == 1L) {
                storedVfsVersion = stream.readLong();
                index = null;
            }
            else if (version == CURRENT_VERSION) {
                storedVfsVersion = stream.readLong();
                index = stream.readLong();
            }
            else {
                // we can assume that small numbers are dirty files queue version and not vfs version
                // because vfs version is vfs creation timestamp that is System.currentTimeMillis()
                storedVfsVersion = version;
                index = null;
            }
            if (currentVfsVersion != null && storedVfsVersion != currentVfsVersion) {
                String message = "Discarding dirty files queue " + queueFile + " because vfs version changed: old=" + storedVfsVersion
                    + ", new=" + currentVfsVersion;
                LOG.debug(message);
                return new IndexingQueueReadResult(new IntArrayList(), null, new OrphanDirtyFilesQueueDiscardReason(message));
            }
            while (stream.available() > 0) {
                fileIds.add(stream.readInt());
            }
            LOG.debug("Dirty file ids read. Size: " + fileIds.size() + ". Index: " + index + " Path: " + queueFile + ".");
            return new IndexingQueueReadResult(fileIds, index, null);
        }
        catch (NoSuchFileException e) {
            error = e;
        }
        catch (EOFException e) {
            error = e;
        }
        catch (IOException e) {
            LOG.info(e);
            error = e;
        }
        return new IndexingQueueReadResult(new IntArrayList(), null, new OrphanDirtyFilesQueueDiscardReason(error.toString()));
    }

    public static void storeIndexingQueue(Path queueFile, IntList fileIds, long index, long vfsVersion) {
        storeIndexingQueue(queueFile, fileIds, index, vfsVersion, CURRENT_VERSION);
    }

    public static void storeIndexingQueue(Path queueFile, IntList fileIds, long index, long vfsVersion, long version) {
        try {
            if (fileIds.isEmpty()) {
                Files.deleteIfExists(queueFile);
            }
            Files.createDirectories(queueFile.getParent());
            try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(queueFile)))) {
                if (version > 0) {
                    stream.writeLong(version);
                }
                stream.writeLong(vfsVersion);
                stream.writeLong(index);
                for (int i = 0; i < fileIds.size(); i++) {
                    stream.writeInt(fileIds.getInt(i));
                }
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        if (isUnittestMode()) {
            StringBuilder idsToPaths = new StringBuilder();
            for (int i = 0; i < fileIds.size(); i++) {
                int fileId = fileIds.getInt(i);
                idsToPaths.append(fileId).append('=').append(StaleIndexesChecker.getStaleRecordOrExceptionMessage(fileId)).append(", ");
            }
            LOG.debug("Dirty file ids stored. Size: " + fileIds.size() + ". Index: " + index + " Path: " + queueFile
                + ". Ids & filenames: " + idsToPaths.substring(0, Math.min(300, idsToPaths.length())));
        }
        else {
            LOG.debug("Dirty file ids stored. Size: " + fileIds.size() + ". Index: " + index + " Path: " + queueFile);
        }
    }
}
