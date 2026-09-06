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
import consulo.it.index.ScanningTestSupport.TestScans;
import consulo.it.internal.HeadlessFilePropertyPusher;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static consulo.it.index.ScanningTestSupport.TIMEOUT_SECONDS;
import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.allowOnlyTestScans;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.fullScan;
import static consulo.it.index.ScanningTestSupport.openProject;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * File properties are pushed inside the scan itself ({@code PushingUtil}), so one scan is enough to persist a
 * pusher's value for every file and offers each file to the pusher exactly once.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class PushingInScanTest {
    private static final int FILES = 10;

    @Test
    public void singleScanPushesPropertiesOnce(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-pushing-in-scan");
        Path src = createSandFiles(directory, FILES, "Pushed");
        Project project = openProject(application, projectManager, directory);
        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);
        awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        HeadlessFilePropertyPusher.setEnabled(true);
        try {
            TestScans scans = allowOnlyTestScans(project, disposable);
            Module module = addContentRoot(project, directory);
            awaitIdle(project);
            VirtualFile srcFile = findFile(src);
            HeadlessFilePropertyPusher.reset();

            scans.queue(fullScan(project, "pushing")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            awaitSmart(dumbService);
            awaitIdle(project);

            String expected = HeadlessFilePropertyPusher.moduleValue(module);
            for (int i = 0; i < FILES; i++) {
                VirtualFile file = srcFile.findChild("file" + i + ".sand");
                assertThat(file).isNotNull();
                assertThat(HeadlessFilePropertyPusher.getPersistedValue(file))
                    .as("the module value must be persisted for %s after one scan", file.getName())
                    .isEqualTo(expected);
                assertThat(file.getUserData(HeadlessFilePropertyPusher.KEY)).isEqualTo(expected);
                assertThat(HeadlessFilePropertyPusher.getAcceptCount(file))
                    .as("%s must be offered to the pusher by exactly one traversal", file.getName())
                    .isEqualTo(1);
            }
        }
        finally {
            HeadlessFilePropertyPusher.setEnabled(false);
            Disposer.dispose(disposable);
        }
    }
}
