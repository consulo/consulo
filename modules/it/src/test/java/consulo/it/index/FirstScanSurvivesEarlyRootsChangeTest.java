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
import consulo.it.index.ScanningTestSupport.RecordedScans;
import consulo.language.index.impl.internal.UnindexedFilesScanner;
import consulo.language.index.impl.internal.UnindexedFilesScannerStartup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitScanParameters;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.recordScans;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A roots change arriving while the scan queued on project open is still running used to cancel that scan without ever
 * requeueing it, leaving the project permanently unindexed. The first scan must now survive such a change, whichever of
 * the two wins the race.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class FirstScanSurvivesEarlyRootsChangeTest {
    private static final int FILES = 5;

    @Test
    public void rootsChangeDuringFirstScanKeepsItAlive(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-first-scan-roots-change");
        createSandFiles(directory, FILES, "Early");
        Project project = openProject(application, projectManager, directory);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            addContentRoot(project, directory);

            awaitSmart(DumbService.getInstance(project));
            awaitIdle(project);

            assertThat(UnindexedFilesScannerStartup.isFirstProjectScanningPerformed(project))
                .as("the scan queued on project open must not be lost by an early roots change")
                .isTrue();

            for (UnindexedFilesScanner scanner : scans.scanners()) {
                awaitScanParameters(scanner);
            }

            for (int i = 0; i < FILES; i++) {
                String name = "Early" + i;
                waitFor(name + " must be findable in the stub index", () -> findClasses(project, name).size() == 1);
            }
        }
        finally {
            Disposer.dispose(disposable);
        }
    }
}
