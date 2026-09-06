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
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.index.ScanningTestSupport.DumbModeEvents;
import consulo.it.index.ScanningTestSupport.RecordedScans;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.index.impl.internal.UnindexedFilesScannerExecutorImpl;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static consulo.it.index.ScanningTestSupport.TIMEOUT_SECONDS;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitScanParameters;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createModule;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.heldFullScan;
import static consulo.it.index.ScanningTestSupport.isFullScan;
import static consulo.it.index.ScanningTestSupport.isPartialScan;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.recordScans;
import static consulo.it.index.ScanningTestSupport.removeModule;
import static consulo.it.index.ScanningTestSupport.scanOrigins;
import static consulo.it.index.ScanningTestSupport.scanReason;
import static consulo.it.index.ScanningTestSupport.scansNothing;
import static consulo.it.index.ScanningTestSupport.subscribeDumbModeEvents;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A change of the project root model no longer restarts the whole indexing: {@code EntityIndexingService} turns the
 * {@code RootsChangeRescanningInfo}s carried by the {@code rootsChanged} event into a scan limited to the entities that
 * actually changed, and a change which needs no rescan at all produces a scanning task which does no work.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class RootsChangeRescanningTest {
    private static final int FILES = 5;

    @Test
    public void moduleContentRootChangeScansOnlyThatModule(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-roots-change-module");
        createSandFiles(directory, FILES, "Mod");
        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            Module module = addContentRoot(project, directory);
            awaitIdle(project);

            assertThat(scans.scanners()).as("the roots change must queue a scanning task").isNotEmpty();
            assertNoFullScan(scans);

            List<IndexableSetOrigin> origins = scans.allOrigins();
            assertThat(origins).as("the roots change must scan the module content roots").isNotEmpty();
            assertThat(origins).allMatch(ModuleRootOrigin.class::isInstance, "must be a module root origin");
            assertThat(moduleOrigins(origins)).containsExactly(module);

            assertClassesIndexed(project, "Mod");
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void libraryRootChangeScansOnlyThatLibrary(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-roots-change-library");
        Path firstRoot = createSandFiles(Files.createTempDirectory("consulo-it-roots-change-library-first"), FILES, "LibFirst");
        Path secondRoot = createSandFiles(Files.createTempDirectory("consulo-it-roots-change-library-second"), FILES, "LibSecond");

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);

        VirtualFile firstRootFile = findFile(firstRoot);
        VirtualFile secondRootFile = findFile(secondRoot);
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Library library = rootModel.getModuleLibraryTable().createLibrary("sand-library");
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(firstRootFile, BinariesOrderRootType.ID);
            libraryModel.commit();
            rootModel.commit();
        });
        awaitIdle(project);

        Library library = ReadAction.compute(() -> findModuleLibrary(module, "sand-library"));
        assertThat(library).as("the module library must survive the root model commit").isNotNull();

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            WriteAction.run(() -> {
                Library.ModifiableModel libraryModel = library.getModifiableModel();
                libraryModel.addRoot(secondRootFile, BinariesOrderRootType.ID);
                libraryModel.commit();
            });
            awaitIdle(project);

            assertThat(scans.scanners()).as("the library root change must queue a scanning task").isNotEmpty();
            assertNoFullScan(scans);

            assertThat(scans.allOrigins())
                .as("the library root change must scan that library")
                .anyMatch(origin -> origin instanceof LibraryOrigin libraryOrigin && library.equals(libraryOrigin.getLibrary()));
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void changeWithoutRescanDoesNoScanningWork(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-roots-change-no-rescan");
        createSandFiles(directory, FILES, "NoRescan");
        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        addContentRoot(project, directory);
        awaitIdle(project);

        Path extraDirectory = Files.createTempDirectory("consulo-it-roots-change-no-rescan-extra");
        Module extra = WriteAction.compute(() -> createModule(project, "extra", extraDirectory));
        awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            DumbModeEvents dumbModeEvents = subscribeDumbModeEvents(project);
            removeModule(project, extra);
            awaitIdle(project);

            for (UnindexedFilesScanner scanner : scans.scanners()) {
                awaitScanParameters(scanner);
                assertThat(scansNothing(scanner)).as("%s must not scan anything", scanner).isTrue();
            }
            assertThat(dumbModeEvents.entered()).as("a change which needs no rescan must not enter dumb mode").isZero();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void mergedRootsChangesQueueASingleScan(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-roots-change-merge");
        Path alphaDirectory = Files.createTempDirectory("consulo-it-roots-change-merge-alpha");
        Path betaDirectory = Files.createTempDirectory("consulo-it-roots-change-merge-beta");
        createSandFiles(alphaDirectory, FILES, "Alpha");
        createSandFiles(betaDirectory, FILES, "Beta");

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        VirtualFile alphaFile = findFile(alphaDirectory);
        VirtualFile betaFile = findFile(betaDirectory);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            Module[] modules = new Module[2];
            WriteAction.run(() -> ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(() -> {
                modules[0] = createModule(project, "alpha", alphaDirectory);
                addContentRoot(modules[0], alphaFile);
                modules[1] = createModule(project, "beta", betaDirectory);
                addContentRoot(modules[1], betaFile);
            }));
            awaitIdle(project);

            assertThat(scans.scanners()).as("merged roots changes must queue exactly one scanning task").hasSize(1);
            UnindexedFilesScanner scanner = scans.scanners().get(0);
            awaitScanParameters(scanner);
            assertThat(isFullScan(scanner)).as("the merged scan (%s) must not be a full index update", scanReason(scanner)).isFalse();

            List<IndexableSetOrigin> origins = scanOrigins(scanner);
            assertThat(origins).as("the merged scan must be a partial one").isNotNull();
            assertThat(moduleOrigins(origins))
                .as("the merged scan must cover both committed modules")
                .containsExactlyInAnyOrder(modules[0], modules[1]);
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void rootsChangeDoesNotCancelRunningScan(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-roots-change-running");
        createSandFiles(directory, FILES, "Running");
        Path extraDirectory = Files.createTempDirectory("consulo-it-roots-change-running-extra");
        createSandFiles(extraDirectory, FILES, "RunningExtra");

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);
        VirtualFile extraFile = findFile(extraDirectory);
        UnindexedFilesScannerExecutorImpl executor = UnindexedFilesScannerExecutorImpl.getInstance(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            CompletableFuture<Void> gate = new CompletableFuture<>();
            Future<?> held = heldFullScan(project, "held full", gate).queue();
            waitFor("the held scan must be picked up by the executor", () -> executor.isRunning().get() && !executor.hasQueuedTasks());
            scans.clear();

            WriteAction.run(() -> addContentRoot(module, extraFile));

            assertThatThrownBy(() -> held.get(1, TimeUnit.SECONDS))
                .as("a partial roots change must not cancel the running scan")
                .isInstanceOf(TimeoutException.class);
            assertThat(executor.hasQueuedTasks()).as("the roots change scan must wait for the running one").isTrue();

            gate.complete(null);
            held.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            awaitIdle(project);

            assertThat(scans.scanners()).as("the roots change must queue exactly one scanning task").hasSize(1);
            UnindexedFilesScanner scanner = scans.scanners().get(0);
            awaitScanParameters(scanner);
            assertThat(isPartialScan(scanner)).as("the roots change scan (%s) must be a partial one", scanReason(scanner)).isTrue();

            assertClassesIndexed(project, "RunningExtra");
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    private static void assertNoFullScan(RecordedScans scans) throws Exception {
        for (UnindexedFilesScanner scanner : scans.scanners()) {
            awaitScanParameters(scanner);
            assertThat(isFullScan(scanner)).as("%s (%s) must not be a full index update", scanner, scanReason(scanner)).isFalse();
        }
    }

    private static Set<Module> moduleOrigins(List<IndexableSetOrigin> origins) {
        return origins.stream()
            .filter(ModuleRootOrigin.class::isInstance)
            .map(origin -> ((ModuleRootOrigin) origin).getModule())
            .collect(Collectors.toSet());
    }

    private static @Nullable Library findModuleLibrary(Module module, String name) {
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry && name.equals(libraryOrderEntry.getLibraryName())) {
                return libraryOrderEntry.getLibrary();
            }
        }
        return null;
    }

    private static void assertClassesIndexed(Project project, String classPrefix) throws Exception {
        for (int i = 0; i < FILES; i++) {
            String name = classPrefix + i;
            waitFor(name + " must be findable in the stub index", () -> findClasses(project, name).size() == 1);
        }
    }
}
