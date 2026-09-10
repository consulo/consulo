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
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.content.ContentIterator;
import consulo.disposer.Disposable;
import consulo.language.index.impl.internal.ScanningIterators;
import consulo.language.index.impl.internal.ScanningType;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.index.impl.internal.UnindexedFilesScannerExecutorImpl;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.IndexedFile;
import consulo.language.psi.stub.StubIndex;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.project.event.ProjectManagerListener;
import consulo.project.impl.internal.DumbServiceImpl;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.stub.SandIndexKeys;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared plumbing of the scanning integration tests: opening a project over a temp directory, giving it a module
 * with a content root, driving the scanner executor with controllable scanning tasks, and waiting for the project
 * to settle.
 *
 * @author VISTALL
 */
public final class ScanningTestSupport {
    public static final String INDEXING_DEBUG_PROPERTY = "consulo.indexing.dirty.files.debug";

    public static final long TIMEOUT_SECONDS = 60;

    private ScanningTestSupport() {
    }

    public static Project openProject(Application application, ProjectManager projectManager, Path directory) throws Exception {
        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(project).isNotNull();
        return project;
    }

    public static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Closes and disposes the project the way the IDE does, so that the closing half of the indexing state -
     * the per-project dirty files queue and the persistent indexable files filter - is written to disk.
     */
    public static void closeProject(Project project) throws Exception {
        Boolean closed = ProjectManager.getInstance()
            .closeAndDisposeAsync(project, project.getUIAccess())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(closed).as("the project must actually close").isTrue();
        waitFor("the closed project must be disposed", project::isDisposed);
    }

    /**
     * Records the scanning tasks of a project which is not opened yet, by installing the recorder from
     * {@code projectOpened} - which the platform publishes before the startup activities, and therefore before
     * {@code ProjectFileBasedIndexStartupActivity} decides between a full and a dirty-files-only scan.
     */
    public static OpenScans recordScansOfNextOpenedProject(Application application, Disposable disposable) {
        return new OpenScans(application, disposable);
    }

    public static Path createSandFiles(Path directory, int count, String classPrefix) throws Exception {
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        for (int i = 0; i < count; i++) {
            Files.writeString(src.resolve("file" + i + ".sand"), "class " + classPrefix + i + " {}");
        }
        return src;
    }

    public static VirtualFile findFile(Path path) {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).as("%s must be visible in the VFS", path).isNotNull();
        return file;
    }

    public static Module addContentRoot(Project project, Path directory) throws Exception {
        return addContentRoot(project, "main", directory);
    }

    public static Module addContentRoot(Project project, String moduleName, Path directory) throws Exception {
        VirtualFile directoryFile = findFile(directory);
        return WriteAction.compute(() -> {
            Module module = createModule(project, moduleName, directory);
            addContentRoot(module, directoryFile);
            return module;
        });
    }

    public static Module createModule(Project project, String name, Path directory) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
        Module module = moduleModel.newModule(name, directory.toString());
        moduleModel.commit();
        return module;
    }

    public static void addContentRoot(Module module, VirtualFile directoryFile) {
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        rootModel.addContentEntry(directoryFile);
        rootModel.commit();
    }

    public static void removeModule(Project project, Module module) {
        WriteAction.run(() -> {
            ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
            moduleModel.disposeModule(module);
            moduleModel.commit();
        });
    }

    public static void awaitSmart(DumbService dumbService) throws InterruptedException {
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();
    }

    public static boolean isDumb(DumbService dumbService) {
        return ReadAction.compute(dumbService::isDumb);
    }

    public static void awaitIdle(Project project) throws Exception {
        DumbServiceImpl dumbService = (DumbServiceImpl) DumbService.getInstance(project);
        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);
        waitFor(
            "scanning and dumb queue must become idle",
            () -> !executor.isRunning().get()
                && !executor.hasQueuedTasks()
                && !dumbService.hasScheduledTasks()
                && !dumbService.isRunning()
                && !isDumb(dumbService)
        );
    }

    public static void awaitScanningFinished(Project project) throws Exception {
        UnindexedFilesScannerExecutor executor = UnindexedFilesScannerExecutor.getInstance(project);
        waitFor("scanning must finish", () -> !executor.isRunning().get() && !executor.hasQueuedTasks());
    }

    /**
     * Turns on the platform's dirty-file logging for the duration of a test. The tests run the platform in their own
     * process, so setting the property here reaches the indexing code directly and needs nothing from the command line.
     */
    public static void indexingDebug(boolean enabled) {
        if (enabled) {
            System.setProperty(INDEXING_DEBUG_PROPERTY, "true");
        }
        else {
            System.clearProperty(INDEXING_DEBUG_PROPERTY);
        }
    }

    public static void waitFor(String description, BooleanSupplier condition) throws Exception {
        waitFor(description, condition, () -> "");
    }

    /**
     * The diagnostics supplier is evaluated only when the wait times out; it exists so a failure on a machine the
     * author cannot reach reports the state that was actually observed rather than just "expected true but was false".
     */
    public static void waitFor(String description, BooleanSupplier condition, Supplier<String> diagnostics) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }

        String details;
        try {
            details = diagnostics.get();
        }
        catch (Throwable e) {
            details = "diagnostics failed: " + e;
        }
        dumpThreads(description);
        assertThat(condition.getAsBoolean()).as("timed out: %s [%s]", description, details).isTrue();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void dumpThreads(String description) {
        StringBuilder dump = new StringBuilder();
        dump.append("=== thread dump at timeout of: ").append(description)
            .append(" [availableProcessors=").append(Runtime.getRuntime().availableProcessors()).append("]\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            dump.append('"').append(thread.getName()).append("\" ").append(thread.getState()).append('\n');
            StackTraceElement[] frames = entry.getValue();
            for (int i = 0; i < Math.min(frames.length, 30); i++) {
                dump.append("    at ").append(frames[i]).append('\n');
            }
        }
        dump.append("=== end of thread dump\n");
        System.err.print(dump);
    }

    public static Collection<SandClass> findClasses(Project project, String name) {
        return ReadAction.compute(() -> {
            try {
                return StubIndex.getElements(
                    SandIndexKeys.SAND_CLASSES,
                    name,
                    project,
                    GlobalSearchScope.allScope(project),
                    SandClass.class
                );
            }
            catch (IndexNotReadyException e) {
                return List.of();
            }
        });
    }

    public static UnindexedFilesScanner fullScan(Project project, String reason) {
        return new UnindexedFilesScanner(project, reason);
    }

    public static UnindexedFilesScanner fullScan(
        Project project,
        String reason,
        @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> forceReindexingTrigger
    ) {
        return new UnindexedFilesScanner(
            project,
            false,
            false,
            null,
            null,
            forceReindexingTrigger,
            false,
            CompletableFuture.completedFuture(new ScanningIterators(reason))
        );
    }

    public static UnindexedFilesScanner heldFullScan(Project project, String reason, Future<?> startCondition) {
        return new UnindexedFilesScanner(
            project,
            false,
            false,
            startCondition,
            null,
            null,
            false,
            CompletableFuture.completedFuture(new ScanningIterators(reason))
        );
    }

    /**
     * A full scan which behaves as the one issued when a project is opened: it asks
     * {@code ProjectIndexingDependenciesService} for an on-project-open scanning token, which is the only token able to
     * <em>read</em> the persistent per-file indexed flag.
     */
    public static UnindexedFilesScanner projectOpenScan(
        Project project,
        String reason,
        @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> forceReindexingTrigger,
        boolean forceCheckingForOutdatedIndexesUsingFileModCount
    ) {
        return new UnindexedFilesScanner(
            project,
            true,
            false,
            null,
            null,
            forceReindexingTrigger,
            forceCheckingForOutdatedIndexesUsingFileModCount,
            CompletableFuture.completedFuture(new ScanningIterators(reason))
        );
    }

    public static UnindexedFilesScanner partialScan(Project project, String reason, IndexableFilesIterator... iterators) {
        return new UnindexedFilesScanner(project, Arrays.asList(iterators), reason);
    }

    public static TestScans allowOnlyTestScans(Project project, Disposable disposable) {
        return new TestScans(project, disposable);
    }

    public static RecordedScans recordScans(Project project, Disposable disposable) {
        return new RecordedScans(project, disposable);
    }

    public static void awaitScanParameters(UnindexedFilesScanner scanner) throws Exception {
        waitFor("the scanning parameters of " + scanner + " must resolve", () -> scanner.isFullIndexUpdate() != null);
    }

    public static @Nullable String scanReason(UnindexedFilesScanner scanner) {
        return scanner.getIndexingReasonBlocking();
    }

    public static @Nullable List<IndexableSetOrigin> scanOrigins(UnindexedFilesScanner scanner) {
        List<IndexableFilesIterator> iterators = scanner.getPredefinedIndexableFileIteratorsBlocking();
        if (iterators == null) {
            return null;
        }
        return iterators.stream().map(IndexableFilesIterator::getOrigin).toList();
    }

    public static boolean scansNothing(UnindexedFilesScanner scanner) {
        return scanReason(scanner) == null;
    }

    public static boolean isPartialScan(UnindexedFilesScanner scanner) {
        return scanOrigins(scanner) != null;
    }

    public static boolean isFullScan(UnindexedFilesScanner scanner) {
        return Boolean.TRUE.equals(scanner.isFullIndexUpdate());
    }

    public static final class RecordedScans {
        private final List<UnindexedFilesScanner> myScanners = new CopyOnWriteArrayList<>();

        private RecordedScans(Project project, Disposable disposable) {
            UnindexedFilesScannerExecutorImpl.getInstance(project).setTaskFilterInTest(disposable, scanner -> {
                myScanners.add(scanner);
                return true;
            });
        }

        public void clear() {
            myScanners.clear();
        }

        public List<UnindexedFilesScanner> scanners() {
            return List.copyOf(myScanners);
        }

        public List<IndexableSetOrigin> allOrigins() throws Exception {
            List<IndexableSetOrigin> origins = new ArrayList<>();
            for (UnindexedFilesScanner scanner : scanners()) {
                awaitScanParameters(scanner);
                List<IndexableSetOrigin> scannerOrigins = scanOrigins(scanner);
                if (scannerOrigins != null) {
                    origins.addAll(scannerOrigins);
                }
            }
            return origins;
        }
    }

    /**
     * The scanning tasks of a project opened after {@link #recordScansOfNextOpenedProject} was called.
     */
    public static final class OpenScans {
        private final List<UnindexedFilesScanner> myScanners = new CopyOnWriteArrayList<>();
        private final List<Project> myProjects = new CopyOnWriteArrayList<>();

        private OpenScans(Application application, Disposable disposable) {
            application.getMessageBus().connect(disposable).subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
                @Override
                public void projectOpened(Project project, UIAccess uiAccess) {
                    myProjects.add(project);
                    UnindexedFilesScannerExecutorImpl.getInstance(project).setTaskFilterInTest(disposable, scanner -> {
                        myScanners.add(scanner);
                        return true;
                    });
                }
            });
        }

        public List<Project> projects() {
            return List.copyOf(myProjects);
        }

        public List<UnindexedFilesScanner> scanners() {
            return List.copyOf(myScanners);
        }

        public List<UnindexedFilesScanner> fullScans() throws Exception {
            List<UnindexedFilesScanner> full = new ArrayList<>();
            for (UnindexedFilesScanner scanner : scanners()) {
                awaitScanParameters(scanner);
                if (isFullScan(scanner)) {
                    full.add(scanner);
                }
            }
            return full;
        }

        public List<ScanningType> scanningTypes() throws Exception {
            List<ScanningType> types = new ArrayList<>();
            for (UnindexedFilesScanner scanner : scanners()) {
                awaitScanParameters(scanner);
                ScanningType type = scanner.getScanningTypeBlocking();
                if (type != null) {
                    types.add(type);
                }
            }
            return types;
        }
    }

    public static DumbModeEvents subscribeDumbModeEvents(Project project) {
        DumbModeEvents events = new DumbModeEvents();
        project.getMessageBus().connect().subscribe(DumbModeListenerBackgroundable.class, events);
        return events;
    }

    public static final class TestScans {
        private final Set<UnindexedFilesScanner> myAllowed = ConcurrentHashMap.newKeySet();

        private TestScans(Project project, Disposable disposable) {
            UnindexedFilesScannerExecutorImpl.getInstance(project).setTaskFilterInTest(disposable, myAllowed::contains);
        }

        public Future<?> queue(UnindexedFilesScanner scanner) {
            myAllowed.add(scanner);
            return scanner.queue();
        }
    }

    public static final class DumbModeEvents implements DumbModeListenerBackgroundable {
        private final AtomicInteger myEntered = new AtomicInteger();
        private final AtomicInteger myExited = new AtomicInteger();

        @Override
        public void enteredDumbMode() {
            myEntered.incrementAndGet();
        }

        @Override
        public void exitDumbMode() {
            myExited.incrementAndGet();
        }

        public int entered() {
            return myEntered.get();
        }

        public int exited() {
            return myExited.get();
        }
    }

    private static final class TestOrigin implements IndexableSetOrigin {
    }

    public static final class BlockingIterator implements IndexableFilesIterator {
        private final IndexableSetOrigin myOrigin = new TestOrigin();
        private final CountDownLatch myStarted = new CountDownLatch(1);
        private final CountDownLatch myRelease = new CountDownLatch(1);

        @Override
        public String getDebugName() {
            return "blocking test iterator";
        }

        @Override
        public LocalizeValue getIndexingProgressText() {
            return LocalizeValue.localizeTODO(getDebugName());
        }

        @Override
        public LocalizeValue getRootsScanningProgressText() {
            return LocalizeValue.localizeTODO(getDebugName());
        }

        @Override
        public IndexableSetOrigin getOrigin() {
            return myOrigin;
        }

        @Override
        public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
            myStarted.countDown();
            while (true) {
                ProgressManager.checkCanceled();
                try {
                    if (myRelease.await(20, TimeUnit.MILLISECONDS)) {
                        return true;
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ProcessCanceledException(e);
                }
            }
        }

        public void awaitStarted() throws InterruptedException {
            assertThat(myStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("the blocking iterator must be reached").isTrue();
        }

        public boolean isStarted() {
            return myStarted.getCount() == 0;
        }

        public void release() {
            myRelease.countDown();
        }
    }

    public static final class RecordingIterator implements IndexableFilesIterator {
        private final IndexableSetOrigin myOrigin = new TestOrigin();
        private final AtomicInteger myIterations = new AtomicInteger();
        private final Runnable myOnIterate;

        public RecordingIterator() {
            this(() -> {
            });
        }

        public RecordingIterator(Runnable onIterate) {
            myOnIterate = onIterate;
        }

        @Override
        public String getDebugName() {
            return "recording test iterator";
        }

        @Override
        public LocalizeValue getIndexingProgressText() {
            return LocalizeValue.localizeTODO(getDebugName());
        }

        @Override
        public LocalizeValue getRootsScanningProgressText() {
            return LocalizeValue.localizeTODO(getDebugName());
        }

        @Override
        public IndexableSetOrigin getOrigin() {
            return myOrigin;
        }

        @Override
        public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
            myIterations.incrementAndGet();
            myOnIterate.run();
            return true;
        }

        public int getIterations() {
            return myIterations.get();
        }
    }
}
