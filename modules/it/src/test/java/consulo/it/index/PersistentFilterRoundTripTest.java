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
import consulo.language.index.impl.internal.projectFilter.ConcurrentFileIds;
import consulo.language.index.impl.internal.projectFilter.PersistentProjectIndexableFilesFilter;
import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilter;
import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilterHolder;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * The persistent per-project indexable files filter, the second half of the state which lets the reopened project
 * skip its full scan. It is written when the project closes and read back when it opens; a filter which cannot be
 * proven to describe the current VFS must come back empty, so that scanning is not skipped on stale data.
 * <p>
 * {@code fileIdsRoundTripThroughTheStream} and {@code filterOfVersionOneIsRejected} are ports of JetBrains'
 * {@code PersistentProjectIndexableFilesFilterTest}. JetBrains asserts the rejection through the module-visible
 * {@code wasDataLoadedFromDisk}; here the same fact is read through the public filter contract - a rejected filter
 * contains none of the ids that were written.
 * <p>
 * The VFS refreshes and the project close path make the platform fire events and take the write lock on the UI
 * thread in a headless application, which is what the class-level opt-outs are about - see {@code
 * ProjectStateReloadTest}. Any other logged error still fails the tests.
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
public class PersistentFilterRoundTripTest {
    private static final int FILES = 25;

    private static final int VERSION_WITHOUT_VFS_STAMP = 1;
    private static final int CURRENT_VERSION = 2;

    private static final long VFS_STAMP = 4242L;

    /**
     * Port of {@code PersistentProjectIndexableFilesFilterTest."test file ids store and read"} and its {@code 2}
     * variant: the second size is deliberately one bit past a whole 32 bit word, so that the tail word is covered.
     */
    @Test
    public void fileIdsRoundTripThroughTheStream() throws Exception {
        assertFileIdsRoundTrip(1000 * Integer.SIZE);
        assertFileIdsRoundTrip(1000 * Integer.SIZE + 1);
    }

    /**
     * Port of {@code PersistentProjectIndexableFilesFilterTest."test reading filter of version 1"}: version 1 stored
     * no VFS creation stamp, so its ids cannot be proven to belong to the current VFS and the whole filter has to be
     * dropped. The version 2 halves of the test are the controls which show the rejection is about the stamp and not
     * about the reader being unable to read anything at all.
     */
    @Test
    public void filterOfVersionOneIsRejected() throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-filter-versions");

        ProjectIndexableFilesFilter legacy = PersistentProjectIndexableFilesFilter.readIndexableFilesFilter(
            writeFilter(directory.resolve("v1"), VERSION_WITHOUT_VFS_STAMP, 0L, 1, 2, 3),
            VFS_STAMP
        );
        assertThat(legacy.containsFileId(2)).as("a filter without a VFS stamp must not be reused").isFalse();

        ProjectIndexableFilesFilter current = PersistentProjectIndexableFilesFilter.readIndexableFilesFilter(
            writeFilter(directory.resolve("v2"), CURRENT_VERSION, VFS_STAMP, 1, 2, 3),
            VFS_STAMP
        );
        assertThat(current.containsFileId(2)).as("a filter of the current VFS must be reused").isTrue();
        assertThat(current.containsFileId(4)).as("and must not invent ids it never stored").isFalse();

        ProjectIndexableFilesFilter otherVfs = PersistentProjectIndexableFilesFilter.readIndexableFilesFilter(
            writeFilter(directory.resolve("v2-other-vfs"), CURRENT_VERSION, VFS_STAMP + 1, 1, 2, 3),
            VFS_STAMP
        );
        assertThat(otherVfs.containsFileId(2)).as("a filter of a previous VFS must not be reused").isFalse();

        ProjectIndexableFilesFilter missing = PersistentProjectIndexableFilesFilter.readIndexableFilesFilter(
            directory.resolve("absent"),
            VFS_STAMP
        );
        assertThat(missing.containsFileId(2)).as("a missing filter file must read as an empty filter").isFalse();
    }

    @Test
    public void filterIsWrittenOnCloseAndLoadedOnOpen(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-filter-round-trip");
        Path src = createSandFiles(directory, FILES, "Filtered");

        Project first = openProject(application, projectManager, directory);
        addContentRoot(first, directory);
        awaitSmart(DumbService.getInstance(first));
        awaitIdle(first);
        waitFor("the first session must index the classes", () -> !findClasses(first, "Filtered5").isEmpty());

        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        ProjectIndexableFilesFilterHolder holder = fileBasedIndex.getIndexableFilesFilterHolder();

        assertThat(holder.wasDataLoadedFromDisk(first))
            .as("the first session built its filter by scanning, it did not read one")
            .isFalse();

        VirtualFile indexed = findFile(src.resolve("file5.sand"));
        int indexedId = ((VirtualFileWithId) indexed).getId();

        Path outside = Files.createTempDirectory("consulo-it-filter-outside").resolve("outside.sand");
        Files.writeString(outside, "class Outside {}");
        int outsideId = ((VirtualFileWithId) findFile(outside)).getId();

        IdFilter before = FileBasedIndex.getInstance().createProjectIndexableFiles(first);
        assertThat(before).as("the filter must be available in smart mode").isNotNull();
        assertThat(before.containsFileId(indexedId)).as("an indexed project file must be in the filter").isTrue();
        assertThat(before.containsFileId(outsideId)).as("a file outside the project must not be in the filter").isFalse();

        saveProject(first, application);
        closeProject(first);

        Project second = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(second));
        awaitIdle(second);

        assertThat(holder.wasDataLoadedFromDisk(second))
            .as("the reopened project must have loaded the filter written on close")
            .isTrue();

        IdFilter after = FileBasedIndex.getInstance().createProjectIndexableFiles(second);
        assertThat(after).as("the reopened project must publish its filter once scanning finished").isNotNull();
        assertThat(after.containsFileId(indexedId))
            .as("a file which was in the filter before the close must still be in it after the reopen")
            .isTrue();
        assertThat(after.containsFileId(outsideId))
            .as("the loaded filter must still exclude what it excluded before, it is not an accept-everything filter")
            .isFalse();
    }

    private static void assertFileIdsRoundTrip(int fileIdCount) throws Exception {
        ConcurrentFileIds fileIds = new ConcurrentFileIds();
        for (int fileId = 1; fileId <= fileIdCount; fileId++) {
            fileIds.set(fileId, true);
        }

        Path file = Files.createTempDirectory("consulo-it-file-ids").resolve("ids");
        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            fileIds.writeTo(stream);
        }

        ConcurrentFileIds read;
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            read = ConcurrentFileIds.readFrom(stream);
        }

        assertThat(read.getCardinality()).as("%s file ids must survive the round trip", fileIdCount).isEqualTo(fileIdCount);
        assertThat(read.get(fileIdCount)).as("the last file id must survive the round trip").isTrue();
        assertThat(read.get(fileIdCount + 1)).as("no file id may be invented past the last stored one").isFalse();
    }

    private static Path writeFilter(Path file, int version, long vfsStamp, int... fileIds) throws Exception {
        ConcurrentFileIds ids = new ConcurrentFileIds();
        for (int fileId : fileIds) {
            ids.set(fileId, true);
        }

        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file);
             DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(out))) {
            stream.writeInt(version);
            if (version != VERSION_WITHOUT_VFS_STAMP) {
                stream.writeLong(vfsStamp);
            }
            ids.writeTo(stream);
        }
        return file;
    }
}
