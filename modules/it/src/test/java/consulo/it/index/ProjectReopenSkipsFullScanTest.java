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
import consulo.it.AllowWriteLockUnderUIThread;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.ScanningType;
import consulo.language.index.impl.internal.projectFilter.PersistentProjectIndexableFilesFilter;
import consulo.module.ModuleManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static consulo.it.index.ScanningTestSupport.OpenScans;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.closeProject;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.recordScansOfNextOpenedProject;
import static consulo.it.index.ScanningTestSupport.saveProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The headline behaviour of {@code UnindexedFilesScannerStartup.scanAndIndexProjectAfterOpen}: a project which was
 * closed cleanly - so that the per-project dirty files queue and the persistent indexable files filter reached disk -
 * must not scan its whole content again when it is reopened. Only the dirty files left over from the previous session
 * are scanned, and the indexes built by the first session are reused as they are.
 * <p>
 * {@code reopenWithoutPersistedFilterScansEverything} is the negative control of that: with the persistent filter
 * thrown away between the two sessions, and nothing else changed, the reopen falls back to a full scan.
 * <p>
 * {@code fileEditedWhileProjectWasClosedIsReindexed} is the safety property which makes trusting the persistent state
 * legitimate: a file edited while the project was closed is still re-indexed on reopen, even though nothing rescanned
 * the project.
 * <p>
 * The project close path disposes the project inside a write action taken on the UI thread, and the VFS refreshes make
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
public class ProjectReopenSkipsFullScanTest {
    private static final int FILES = 25;

    /**
     * Saving the project before closing it is what carries the module and its content root into the second session -
     * without them the reopened project would have nothing indexable and every assertion below would be vacuous,
     * which is why the module list is asserted too.
     */
    @Test
    public void reopenedProjectDoesNotScanEverythingAgain(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-reopen-skips-scan");
        createSandFiles(directory, FILES, "Reopen");

        Project first = openProject(application, projectManager, directory);
        addContentRoot(first, directory);
        awaitSmart(DumbService.getInstance(first));
        awaitIdle(first);
        waitFor("the first session must index the classes", () -> !findClasses(first, "Reopen5").isEmpty());

        saveProject(first, application);
        closeProject(first);

        Disposable disposable = Disposable.newDisposable();
        try {
            OpenScans scans = recordScansOfNextOpenedProject(application, disposable);

            Project second = openProject(application, projectManager, directory);
            assertThat(scans.projects()).as("the recorder must have seen the reopened project").contains(second);

            awaitSmart(DumbService.getInstance(second));
            awaitIdle(second);

            assertThat(ModuleManager.getInstance(second).getModules())
                .as("the reopened project must have loaded the module of the first session, otherwise nothing is indexable")
                .isNotEmpty();

            assertThat(scans.scanners())
                .as("the reopened project must still run its on-open scanning task")
                .isNotEmpty();
            assertThat(scans.fullScans())
                .as("no full scan may run on reopen, the persistent filter and the dirty queue answer instead")
                .isEmpty();
            assertThat(scans.scanningTypes())
                .as("the on-open scan must be the dirty-files-only one")
                .contains(ScanningType.PARTIAL_ON_PROJECT_OPEN);

            waitFor(
                "the classes indexed by the first session must still be findable without any rescan",
                () -> !findClasses(second, "Reopen5").isEmpty()
            );
            assertThat(findClasses(second, "Reopen" + (FILES - 1)))
                .as("every class of the first session must survive the reopen")
                .isNotEmpty();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    /**
     * The negative control of {@link #reopenedProjectDoesNotScanEverythingAgain}: the very same fixture, with the
     * persistent filter thrown away between the two sessions, must fall back to a full scan. Without it a broken
     * recorder - or a scan which never reaches the executor at all - would let the positive assertion pass on nothing.
     * Deleting the persisted filter is the only difference to the test above.
     */
    @Test
    public void reopenWithoutPersistedFilterScansEverything(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-reopen-without-filter");
        createSandFiles(directory, FILES, "NoFilter");

        Project first = openProject(application, projectManager, directory);
        addContentRoot(first, directory);
        awaitSmart(DumbService.getInstance(first));
        awaitIdle(first);
        waitFor("the first session must index the classes", () -> !findClasses(first, "NoFilter5").isEmpty());

        saveProject(first, application);
        closeProject(first);

        PersistentProjectIndexableFilesFilter.deletePersistentIndexableFilesFilters();

        Disposable disposable = Disposable.newDisposable();
        try {
            OpenScans scans = recordScansOfNextOpenedProject(application, disposable);

            Project second = openProject(application, projectManager, directory);
            awaitSmart(DumbService.getInstance(second));
            awaitIdle(second);

            assertThat(scans.fullScans())
                .as("without a persisted indexable files filter the reopen must scan the whole project again")
                .isNotEmpty();
            assertThat(scans.scanningTypes())
                .as("and it must be the on-project-open full scan")
                .contains(ScanningType.FULL_ON_PROJECT_OPEN);
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    /**
     * The file is rewritten while no project is open for it, so nothing observed the change; the reopen skips the full
     * scan and the persistent flag still claims the file is indexed. Only the VFS refresh can notice, and it must lead
     * to the file being re-indexed. Both the new and the old content are checked in the same poll, so a non-empty
     * answer for the new class proves the index was ready when the empty answer for the old one was produced.
     */
    @Test
    public void fileEditedWhileProjectWasClosedIsReindexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-reopen-edited-file");
        Path src = createSandFiles(directory, FILES, "Before");

        Project first = openProject(application, projectManager, directory);
        addContentRoot(first, directory);
        awaitSmart(DumbService.getInstance(first));
        awaitIdle(first);
        waitFor("the first session must index the classes", () -> !findClasses(first, "Before5").isEmpty());

        saveProject(first, application);
        closeProject(first);

        Files.writeString(src.resolve("file5.sand"), "class After5 {}");

        Disposable disposable = Disposable.newDisposable();
        try {
            OpenScans scans = recordScansOfNextOpenedProject(application, disposable);

            Project second = openProject(application, projectManager, directory);
            awaitSmart(DumbService.getInstance(second));
            awaitIdle(second);

            assertThat(scans.fullScans())
                .as("the edited file must be picked up without falling back to a full scan")
                .isEmpty();

            VirtualFile srcFile = findFile(src);
            srcFile.refresh(false, true);

            waitFor(
                "the file edited while the project was closed must be re-indexed on reopen",
                () -> !findClasses(second, "After5").isEmpty() && findClasses(second, "Before5").isEmpty()
            );

            assertThat(findClasses(second, "Before4"))
                .as("files which did not change must keep their indexes")
                .isNotEmpty();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }
}
