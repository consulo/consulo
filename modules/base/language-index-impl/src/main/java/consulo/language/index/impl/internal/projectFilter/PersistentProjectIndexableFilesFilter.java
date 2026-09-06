// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.projectFilter;

import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.util.io.FileUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

class PersistentProjectIndexableFilesFilterFactory extends ProjectIndexableFilesFilterFactory {
    @Override
    ProjectIndexableFilesFilter create(Project project, long currentVfsCreationTimestamp) {
        Path file = PersistentProjectIndexableFilesFilter.getFiltersDir().resolve(ProjectUtil.getProjectCacheFileName(project));
        return PersistentProjectIndexableFilesFilter.readIndexableFilesFilter(file, currentVfsCreationTimestamp);
    }
}

/**
 * Note about Invalidate Caches:
 * This filter doesn't require explicit caches invalidation because during invalidation AppIndexingDependenciesService
 * advances token which then causes filter to be rebuilt during next scanning
 * (see UnindexedFilesScanner.isIndexableFilesFilterUpToDate)
 */
public class PersistentProjectIndexableFilesFilter extends IncrementalProjectIndexableFilesFilter {
    private static final Logger LOG = Logger.getInstance(PersistentProjectIndexableFilesFilterFactory.class);

    private static final int VERSION = 2;

    private final boolean myWasDataLoadedFromDisk;

    PersistentProjectIndexableFilesFilter(boolean wasDataLoadedFromDisk, ConcurrentFileIds fileIds) {
        super(fileIds);
        myWasDataLoadedFromDisk = wasDataLoadedFromDisk;
    }

    static Path getFiltersDir() {
        return ContainerPathManager.get().getIndexRoot().toPath().resolve("index-file-filters");
    }

    /**
     * We don't have to delete these files explicitly
     * They won't be used anyway because AppIndexingDependenciesService.getCurrent
     * will be changed and isIndexableFilesFilterUpToDate will return false.
     * <p>
     * But it's better to do it explicitly.
     */
    public static void deletePersistentIndexableFilesFilters() {
        FileUtil.deleteWithRenaming(getFiltersDir().toFile());
    }

    public static ProjectIndexableFilesFilter readIndexableFilesFilter(Path file, long currentVfsCreationTimestamp) {
        try {
            PersistentProjectIndexableFilesFilter filter;
            try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                int version = stream.readInt(); // version
                long vfsCreationTimestamp;
                if (version == 1) {
                    vfsCreationTimestamp = -1; // vfsCreationTimestamp was not saved in version 1
                }
                else if (version == 2) {
                    vfsCreationTimestamp = stream.readLong();
                }
                else {
                    LOG.error("Unknown PersistentProjectIndexableFilesFilter version " + version);
                    vfsCreationTimestamp = -1;
                }

                filter = vfsCreationTimestamp == currentVfsCreationTimestamp
                    ? new PersistentProjectIndexableFilesFilter(true, ConcurrentFileIds.readFrom(stream))
                    : null;
            }
            Files.deleteIfExists(file);
            if (filter != null) {
                return filter;
            }
        }
        catch (NoSuchFileException ignored) {
        }
        catch (EOFException ignored) {
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return new PersistentProjectIndexableFilesFilter(false, new ConcurrentFileIds());
    }

    @Override
    boolean wasDataLoadedFromDisk() {
        return myWasDataLoadedFromDisk;
    }

    @Override
    void onProjectClosing(Project project, long vfsCreationStamp) {
        Path file = getFiltersDir().resolve(ProjectUtil.getProjectCacheFileName(project));
        try {
            if (myFileIds.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(file.getParent());
            try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                stream.writeInt(VERSION);
                stream.writeLong(vfsCreationStamp);
                myFileIds.writeTo(stream);
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }
}
