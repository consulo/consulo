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
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.IndexingFlag;
import consulo.language.index.impl.internal.PerProjectIndexingQueue;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.index.impl.internal.dependencies.AppIndexingDependenciesService;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.dependencies.IsFileChangedResult;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.dependencies.ReadWriteFileIndexingStampImpl;
import consulo.language.index.impl.internal.dependencies.ScanningRequestToken;
import consulo.language.psi.stub.IndexedFile;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.impl.internal.mapped.MappedFileStorageHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static consulo.it.index.ScanningTestSupport.TIMEOUT_SECONDS;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.allowOnlyTestScans;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.fullScan;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.projectOpenScan;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code IndexingFlag} keeps, per file id and outside the VFS records, the {@code FileIndexingStamp} a file was last
 * indexed with. {@code UnindexedFilesFinder} reads it before it takes a read action, so a file which is already indexed
 * costs nothing but a single long read on the following scans (IJPL-229).
 * <p>
 * The flag is only <em>read</em> through a token issued by
 * {@link ProjectIndexingDependenciesService#newScanningTokenOnProjectOpen}: a plain scanning token is write-only by
 * design, so an ordinary rescan keeps relying on {@code IndexingStamp}. These tests therefore drive the second scan
 * through {@link ScanningTestSupport#projectOpenScan}.
 * <p>
 * The fast path is observed through the scanner's {@code forceReindexingTrigger}: the finder consults the trigger right
 * after a positive flag read, which happens <em>before</em> {@code AccessRule.read}, and otherwise only from inside that
 * read action. Recording {@link Application#isReadAccessAllowed()} at the first invocation for a file therefore tells
 * which of the two paths the finder took.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class IndexingFlagTest {
    private static final int FILES = 12;
    private static final String FLAG_ATTRIBUTE_ID = "indexing.flag";

    @Test
    public void warmRescanSkipsIndexedFilesBeforeTakingAReadAction(Application application, ProjectManager projectManager)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-indexing-flag-warm");
        Path src = createSandFiles(directory, FILES, "Warm");

        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        addContentRoot(project, directory);
        awaitIdle(project);
        awaitSmart(dumbService);

        for (int i = 0; i < FILES; i++) {
            String name = "Warm" + i;
            waitFor(name + " must be indexed by the first scan", () -> !findClasses(project, name).isEmpty());
        }

        List<VirtualFile> files = sandFiles(src);
        ProjectIndexingDependenciesService dependencies = ProjectIndexingDependenciesService.getInstance(project);
        assertFlagged(dependencies, files, "the first scan and the indexing after it must write the persistent indexed flag");

        Disposable disposable = Disposable.newDisposable();
        try {
            ScanningTestSupport.TestScans scans = allowOnlyTestScans(project, disposable);
            PerProjectIndexingQueue queue = PerProjectIndexingQueue.getInstance(project);

            FastPathProbe plainProbe = new FastPathProbe(application);
            scanWithoutFlushing(queue, scans, fullScan(project, "warm rescan", plainProbe));

            assertThat(queue.getQueuedFiles().getRequests())
                .as("a plain rescan of an already indexed project must schedule nothing for re-indexing")
                .isEmpty();

            assertThat(plainProbe.namesSeen())
                .as("the finder must have looked at every .sand file")
                .containsAll(sandFileNames());

            assertThat(plainProbe.namesUnderReadAction(sandFileNames()))
                .as("a plain scanning token is write-only, so the flag cannot short-circuit this scan")
                .containsExactlyInAnyOrderElementsOf(sandFileNames());

            FastPathProbe projectOpenProbe = new FastPathProbe(application);
            scanWithoutFlushing(queue, scans, projectOpenScan(project, "warm rescan on project open", projectOpenProbe, false));

            assertThat(queue.getQueuedFiles().getRequests())
                .as("a scan of an already indexed project must schedule nothing for re-indexing")
                .isEmpty();

            assertThat(projectOpenProbe.namesSeen())
                .as("the finder must have looked at every .sand file")
                .containsAll(sandFileNames());

            assertThat(projectOpenProbe.namesUnderReadAction(sandFileNames()))
                .as("an already indexed file must be recognized before any read action is taken")
                .isEmpty();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void invalidateAllStampsMakesEveryFileMissOnce(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-indexing-flag-invalidate");
        Path src = createSandFiles(directory, FILES, "Invalidated");

        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        addContentRoot(project, directory);
        awaitIdle(project);
        awaitSmart(dumbService);

        for (int i = 0; i < FILES; i++) {
            String name = "Invalidated" + i;
            waitFor(name + " must be indexed by the first scan", () -> !findClasses(project, name).isEmpty());
        }

        List<VirtualFile> files = sandFiles(src);
        ProjectIndexingDependenciesService dependencies = ProjectIndexingDependenciesService.getInstance(project);
        assertFlagged(dependencies, files, "the first scan and the indexing after it must write the persistent indexed flag");

        AppIndexingDependenciesService.getInstance().invalidateAllStamps("IndexingFlagTest");

        ScanningRequestToken afterInvalidation = dependencies.getReadOnlyTokenForTest();
        for (VirtualFile file : files) {
            FileIndexingStamp stamp = afterInvalidation.getFileIndexingStamp(file);
            assertThat(IndexingFlag.isFileIndexed(file, stamp))
                .as("%s must lose its indexed flag once all the stamps are invalidated", file.getName())
                .isFalse();
            assertThat(IndexingFlag.isFileChanged(file, stamp))
                .as("%s must be reported as changed once all the stamps are invalidated", file.getName())
                .isEqualTo(IsFileChangedResult.YES);
        }

        Disposable disposable = Disposable.newDisposable();
        try {
            ScanningTestSupport.TestScans scans = allowOnlyTestScans(project, disposable);
            PerProjectIndexingQueue queue = PerProjectIndexingQueue.getInstance(project);
            FastPathProbe probe = new FastPathProbe(application);

            scanWithoutFlushing(queue, scans, projectOpenScan(project, "rescan after invalidation", probe, true));

            assertThat(names(queue.getQueuedFiles().getRequests()))
                .as("every file must be queued for indexing again after the stamps were invalidated")
                .containsAll(sandFileNames());

            assertThat(probe.namesUnderReadAction(sandFileNames()))
                .as("a file whose flag was invalidated must fall through to the full evaluation under a read action")
                .containsExactlyInAnyOrderElementsOf(sandFileNames());

            queue.clear();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    /**
     * A real IDE restart cannot be simulated headlessly, so the persistence is exercised the way {@code IndexingFlag}
     * itself does it on a VFS reload: the mapped storage is closed and transparently reopened from disk on the next read.
     */
    @Test
    public void flagSurvivesAttributeStorageReload() throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-indexing-flag-reload");
        Path file = directory.resolve("reload.sand");
        Files.writeString(file, "class Reload {}");
        VirtualFile virtualFile = findFile(file);

        FileIndexingStamp stamp = new ReadWriteFileIndexingStampImpl(0x5AFEC0DEL);
        IndexingFlag.setFileIndexed(virtualFile, stamp);
        assertThat(IndexingFlag.isFileIndexed(virtualFile, stamp))
            .as("the flag must be readable right after it is written")
            .isTrue();

        assertThat(openFlagStorages())
            .as("the flag must be served by an open mapped storage while it is being written")
            .isNotEmpty();

        IndexingFlag.reloadAttributes();

        assertThat(openFlagStorages())
            .as("reloading the attributes must really close and deregister the mapped storage")
            .isEmpty();

        assertThat(IndexingFlag.isFileIndexed(virtualFile, stamp))
            .as("the flag must be reread from disk after the attribute storage was closed")
            .isTrue();

        IndexingFlag.close();

        assertThat(IndexingFlag.isFileIndexed(virtualFile, stamp))
            .as("the flag must survive a full close of the attribute storage")
            .isTrue();

        IndexingFlag.cleanProcessingFlag(virtualFile);
        assertThat(IndexingFlag.isFileIndexed(virtualFile, stamp))
            .as("a cleaned flag must not be reported as indexed")
            .isFalse();
    }

    private static List<Path> openFlagStorages() {
        List<Path> paths = new ArrayList<>();
        Map<Path, MappedFileStorageHelper> registry = MappedFileStorageHelper.registeredStorages();
        synchronized (registry) {
            for (Map.Entry<Path, MappedFileStorageHelper> entry : registry.entrySet()) {
                if (entry.getKey().getFileName().toString().startsWith(FLAG_ATTRIBUTE_ID) && entry.getValue().isOpen()) {
                    paths.add(entry.getKey());
                }
            }
        }
        return paths;
    }

    private static void assertFlagged(ProjectIndexingDependenciesService dependencies, List<VirtualFile> files, String description) {
        ScanningRequestToken token = dependencies.getReadOnlyTokenForTest();
        for (VirtualFile file : files) {
            assertThat(IndexingFlag.isFileIndexed(file, token.getFileIndexingStamp(file)))
                .as("%s: %s", description, file.getName())
                .isTrue();
        }
    }

    private static void scanWithoutFlushing(
        PerProjectIndexingQueue queue,
        ScanningTestSupport.TestScans scans,
        UnindexedFilesScanner scanner
    ) throws Exception {
        AtomicReference<Exception> failure = new AtomicReference<>();
        queue.disableFlushingDuring(() -> {
            try {
                scans.queue(scanner).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                failure.set(e);
            }
            return null;
        });
        Exception exception = failure.get();
        if (exception != null) {
            throw exception;
        }
    }

    private static List<VirtualFile> sandFiles(Path src) {
        List<VirtualFile> files = new ArrayList<>();
        for (int i = 0; i < FILES; i++) {
            files.add(findFile(src.resolve("file" + i + ".sand")));
        }
        return files;
    }

    private static Set<String> sandFileNames() {
        Set<String> names = new TreeSet<>();
        for (int i = 0; i < FILES; i++) {
            names.add("file" + i + ".sand");
        }
        return names;
    }

    private static Set<String> names(Set<VirtualFile> files) {
        Set<String> names = new TreeSet<>();
        for (VirtualFile file : files) {
            names.add(file.getName());
        }
        return names;
    }

    /**
     * Records, per file, whether a read action was already held the first time the finder consulted the trigger.
     * Always answers {@code false}, so the scan it observes behaves as a scan without a trigger.
     */
    private static final class FastPathProbe implements BiPredicate<IndexedFile, FileIndexingStamp> {
        private final Application myApplication;
        private final Map<String, Boolean> myFirstCall = new ConcurrentHashMap<>();

        private FastPathProbe(Application application) {
            myApplication = application;
        }

        @Override
        public boolean test(IndexedFile file, FileIndexingStamp stamp) {
            myFirstCall.putIfAbsent(file.getFileName(), myApplication.isReadAccessAllowed());
            return false;
        }

        private Set<String> namesSeen() {
            return new TreeSet<>(myFirstCall.keySet());
        }

        private Set<String> namesUnderReadAction(Set<String> interesting) {
            Set<String> result = new TreeSet<>();
            for (Map.Entry<String, Boolean> entry : myFirstCall.entrySet()) {
                if (entry.getValue() && interesting.contains(entry.getKey())) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }
    }
}
