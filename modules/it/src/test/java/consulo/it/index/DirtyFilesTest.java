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
package consulo.it.index;

import consulo.application.Application;
import consulo.it.AllowLogError;
import consulo.it.AllowWriteLockUnderUIThread;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.PersistentDirtyFilesQueue;
import consulo.language.index.impl.internal.ProjectDirtyFilesQueue;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.ManagingFS;
import consulo.component.messagebus.MessageBusConnection;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFileWithId;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.closeProject;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.saveProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The per-project dirty files queue: the ids of files whose indexes are known to be out of date when the project is
 * closed. It is what makes skipping the full scan on the next open safe - the reopened project scans exactly those
 * files instead of everything.
 * <p>
 * The two serialization tests are ports of JetBrains' {@code PersistentDirtyFilesQueueTest}.
 * <p>
 * The project close path disposes the project inside a write action taken on the UI thread, and the VFS refresh makes
 * the platform fire VFS events on the UI thread - see {@code ProjectStateReloadTest}; the sand plugin also registers
 * actions into UI groups which do not exist in a headless application. That is what the class-level opt-outs are
 * about; any other logged error still fails the tests.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowWriteLockUnderUIThread
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class DirtyFilesTest {
    private static final int FILES = 25;

    private static final long VFS_VERSION = 987L;
    private static final long INDEX_IN_ORPHAN_QUEUE = 42L;

    /**
     * Port of {@code PersistentDirtyFilesQueueTest."test store and load if vfs version"}: the queue round-trips only
     * while the VFS it was written against is still the current one. A VFS which was recreated meanwhile invalidates
     * every recorded id, because ids identify records of that VFS and nothing else.
     */
    @Test
    public void queueRoundTripsOnlyWithinTheSameVfs() throws Exception {
        Path queueFile = Files.createTempDirectory("consulo-it-dirty-queue").resolve("queue");
        PersistentDirtyFilesQueue.storeIndexingQueue(queueFile, IntArrayList.of(1, 2, 3), INDEX_IN_ORPHAN_QUEUE, VFS_VERSION);

        PersistentDirtyFilesQueue.IndexingQueueReadResult sameVfs =
            PersistentDirtyFilesQueue.readIndexingQueue(queueFile, VFS_VERSION);
        assertThat(sameVfs.fileIds().toIntArray()).as("the stored ids must be read back verbatim").containsExactly(1, 2, 3);
        assertThat(sameVfs.index()).as("the position in the orphan queue must be read back").isEqualTo(INDEX_IN_ORPHAN_QUEUE);
        assertThat(sameVfs.orphanQueueDiscardReason()).as("nothing to discard").isNull();

        PersistentDirtyFilesQueue.IndexingQueueReadResult otherVfs =
            PersistentDirtyFilesQueue.readIndexingQueue(queueFile, VFS_VERSION + 1);
        assertThat(otherVfs.fileIds().toIntArray()).as("ids of a previous VFS must not be reused").isEmpty();
        assertThat(otherVfs.index()).isNull();
        assertThat(otherVfs.orphanQueueDiscardReason()).as("discarding the queue must be explained").isNotNull();
    }

    /**
     * Port of {@code PersistentDirtyFilesQueueTest."test unversioned file is read correctly"}: a queue file written
     * before the format carried a version is still readable, with every long in it decoded as a pair of file ids.
     */
    @Test
    public void unversionedQueueFileIsStillReadable() throws Exception {
        Path queueFile = Files.createTempDirectory("consulo-it-dirty-queue-legacy").resolve("queue");
        PersistentDirtyFilesQueue.storeIndexingQueue(queueFile, IntArrayList.of(1, 2, 3), INDEX_IN_ORPHAN_QUEUE, 0L, 0L);

        PersistentDirtyFilesQueue.IndexingQueueReadResult read = PersistentDirtyFilesQueue.readIndexingQueue(queueFile, null);
        assertThat(read.fileIds().toIntArray())
            .as("the two halves of the index long are read as ids, the version long is the VFS version")
            .containsExactly(0, (int) INDEX_IN_ORPHAN_QUEUE, 1, 2, 3);
        assertThat(read.index()).as("an unversioned file carries no orphan queue position").isNull();
    }

    /**
     * A single change stays below the count at which the platform drains pending VFS events on a background worker, so
     * the file is still recorded as dirty when the project closes - which is why no index may be queried between the
     * change and the close, since reading an index drains the pending change and indexes the file.
     * <p>
     * After the reopen no VFS refresh is performed on purpose. That the queue was actually consumed is asserted through
     * the ids disappearing from the dirty files of the reopened project: they are put there by {@code registerProject}
     * from the persisted queue, and only the completion of the on-open scanning job takes them out again.
     */
    @Test
    public void dirtyFileIsPersistedOnCloseAndIndexedOnNextOpen(Application application, ProjectManager projectManager)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-dirty-files");
        Path src = createSandFiles(directory, FILES, "Clean");

        Project first = openProject(application, projectManager, directory);
        addContentRoot(first, directory);
        awaitSmart(DumbService.getInstance(first));
        awaitIdle(first);
        waitFor("the first session must index the classes", () -> !findClasses(first, "Clean0").isEmpty());
        saveProject(first, application);

        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        Path queueFile = PersistentDirtyFilesQueue.getQueueFile(first);

        VirtualFile changed = findFile(src.resolve("file0.sand"));
        int changedId = ((VirtualFileWithId) changed).getId();
        int untouchedId = ((VirtualFileWithId) findFile(src.resolve("file1.sand"))).getId();

        List<String> observedVfsChanges = Collections.synchronizedList(new ArrayList<>());
        MessageBusConnection vfsConnection = application.getMessageBus().connect();
        vfsConnection.subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    VirtualFile eventFile = event.getFile();
                    if (eventFile != null) {
                        observedVfsChanges.add(eventFile.getPath());
                    }
                }
            }
        });

        try {
            Files.writeString(src.resolve("file0.sand"), "class Dirty0 { int changedOnDisk; }");
            VirtualFileUtil.markDirtyAndRefresh(false, false, false, changed);

            // the headless application runs without a file watcher, so the explicit refresh above is the only thing
            // that can turn the write into a VFS event; if it does not, nothing downstream can see the change
            waitFor(
                "the explicit refresh must make the VFS report the content change",
                () -> observedVfsChanges.contains(changed.getPath()),
                () -> "observedVfsChanges=" + observedVfsChanges + " vfsLength=" + changed.getLength()
            );
        }
        finally {
            vfsConnection.disconnect();
        }

        waitFor(
            "the change must be recorded in the project dirty files",
            () -> fileBasedIndex.getAllDirtyFiles(first).contains(changedId),
            () -> "changedId=" + changedId
                + " projectDirtyIds=" + fileBasedIndex.getAllDirtyFiles(first)
                + " orphanDirtyIds=" + fileBasedIndex.getAllDirtyFiles(null)
                + " inFilter=" + fileBasedIndex.getIndexableFilesFilterHolder().findProjectsForFile(changedId)
                + " vfsLength=" + changed.getLength()
                + " vfsTimeStamp=" + changed.getTimeStamp()
                + " filesToUpdate=" + fileBasedIndex.getFilesToUpdateCollector().getDirtyFiles().getProjectDirtyFiles(first)
        );

        closeProject(first);

        ProjectDirtyFilesQueue persisted = PersistentDirtyFilesQueue.readProjectDirtyFilesQueue(
            queueFile,
            ManagingFS.getInstance().getCreationTimestamp()
        );
        assertThat(persisted.getFileIds())
            .as("the dirty file must survive the project close as a persisted id")
            .contains(changedId);
        assertThat(persisted.getFileIds())
            .as("the queue must hold the files which are actually dirty, not every file of the project")
            .doesNotContain(untouchedId);

        Project second = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(second));
        awaitIdle(second);

        waitFor(
            "the queued dirty file must be re-indexed by the reopened project",
            () -> !findClasses(second, "Dirty0").isEmpty() && findClasses(second, "Clean0").isEmpty()
        );

        assertThat(findClasses(second, "Clean1"))
            .as("files which were not dirty must keep the indexes of the first session")
            .isNotEmpty();

        waitFor(
            "the consumed ids must be dropped from the dirty files of the reopened project",
            () -> !fileBasedIndex.getAllDirtyFiles(second).contains(changedId)
        );
    }
}
