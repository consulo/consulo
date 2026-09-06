// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.events;

import consulo.application.ApplicationManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public final class FilesToUpdateCollector {
    private static final Logger LOG = Logger.getInstance(FilesToUpdateCollector.class);

    //files are duplicated here: they are both in filesToUpdate and in dirtyFiles (already sorted by
    // projects). Maybe it is worth merging that functionality -- so we could always query dirty
    // files per project?
    private final ConcurrentIntObjectMap<FileIndexingRequest> myFilesToUpdate = IntMaps.newConcurrentIntObjectHashMap();

    /**
     * This {@link DirtyFiles} container tracks the ({@link FileIndexingRequest} -> indexing) phase of the pipeline: {@code fileId} is
     * tracked here from the moment {@link FileIndexingRequest} is created for it, until the moment {@link FileIndexingRequest} is
     * processed by the indexing pipeline, and the indexes are updated accordingly.
     * The project(s) owner(s) for a {@code fileId} is typically transferred from the previous phase
     * ({@code ChangedFilesCollector})
     */
    private final DirtyFiles myDirtyFiles = new DirtyFiles();

    public void registerProject(Project project) {
        myDirtyFiles.addProject(project);
    }

    public void unregisterProject(Project project) {
        myDirtyFiles.removeProject(project);
    }

    /**
     * @param containingProjects projects request.file is belong to. Used mostly for diagnostics
     * @param dirtyQueueProjects projects request.file is belong to. Used to actually put the file into
     *                           apt queue(s)
     */
    public void scheduleForUpdate(
        FileIndexingRequest request,
        Set<Project> containingProjects,
        Collection<? extends Project> dirtyQueueProjects
    ) {
        if (!request.isDeleteRequest() && request.getFile().isDirectory()) {
            LOG.warn("Directory was passed for indexing unexpectedly: " + request.getFile().getPath(), new Throwable());
        }
        VirtualFile file = request.getFile();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            if (!request.isDeleteRequest() && containingProjects.isEmpty()) {
                LOG.error("File without project should not be added to FilesToUpdateCollector because it will not be indexed " +
                    "(projects pick own update requests and all delete requests from this collector). " +
                    "File=" + file.getPath());
            }
        }
        int fileId = request.getFileId();
        myDirtyFiles.addFile(dirtyQueueProjects, fileId);
        myFilesToUpdate.put(fileId, request);
    }

    public DirtyFiles getDirtyFiles() {
        return myDirtyFiles;
    }

    public void removeScheduledFileFromUpdate(VirtualFile file) {
        int fileId = FileBasedIndex.getFileId(file);
        FileIndexingRequest alreadyScheduledFile = myFilesToUpdate.get(fileId);
        if (alreadyScheduledFile != null && !alreadyScheduledFile.isDeleteRequest()) {
            myFilesToUpdate.remove(fileId);
            myDirtyFiles.removeFile(fileId);
        }
    }

    public void removeFileIdFromFilesScheduledForUpdate(int fileId) {
        myFilesToUpdate.remove(fileId);
        myDirtyFiles.removeFile(fileId);
    }

    public void clear() {
        myDirtyFiles.clear();
        myFilesToUpdate.clear();
    }

    public Iterator<FileIndexingRequest> getFilesToUpdateAsIterator() {
        return myFilesToUpdate.values().iterator();
    }

    public Collection<FileIndexingRequest> getFilesToUpdate() {
        return myFilesToUpdate.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableCollection(myFilesToUpdate.values());
    }

    public boolean isScheduledForUpdate(VirtualFile file) {
        return containsFileId(FileBasedIndex.getFileId(file));
    }

    public boolean containsFileId(int fileId) {
        return myFilesToUpdate.containsKey(fileId);
    }
}
