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

import consulo.application.WriteAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.index.ScanningTestSupport.RecordedScans;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.recordScans;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Committing a root model without changes went through the module model as a roots change with no changed modules, which
 * is a total rescan of the project. External system imports commit the root model of every module on each project open,
 * so every open of an imported project was rescanned completely.
 *
 * @author VISTALL
 * @since 2026-11-28
 */
@ExtendWith(HeadlessProjectExtension.class)
public class UnchangedRootModelCommitTest {
    @Test
    public void committingAnUnchangedRootModelChangesNoRoots(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-unchanged-commit");
        createSandFiles(directory, 3, "Unchanged");
        Project project = projects.open(directory);
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            AtomicInteger rootsChanges = new AtomicInteger();
            project.getMessageBus().connect(disposable).subscribe(ModuleRootListener.class, new ModuleRootListener() {
                @Override
                public void rootsChanged(ModuleRootEvent event) {
                    rootsChanges.incrementAndGet();
                }
            });

            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                assertThat(rootModel.isChanged()).isFalse();
                rootModel.commit();
            });
            awaitIdle(project);

            assertThat(rootsChanges.get()).as("an unchanged root model must not fire a roots change").isZero();
            assertThat(scans.scanners()).as("an unchanged root model must not queue a scanning").isEmpty();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @Test
    public void committingAChangedRootModelStillChangesRoots(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-changed-commit");
        Path otherRoot = createSandFiles(Files.createTempDirectory("consulo-it-changed-commit-other"), 3, "Changed");
        Project project = projects.open(directory);
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);

        Disposable disposable = Disposable.newDisposable();
        try {
            AtomicInteger rootsChanges = new AtomicInteger();
            project.getMessageBus().connect(disposable).subscribe(ModuleRootListener.class, new ModuleRootListener() {
                @Override
                public void rootsChanged(ModuleRootEvent event) {
                    rootsChanges.incrementAndGet();
                }
            });

            addContentRoot(module, ScanningTestSupport.findFile(otherRoot));
            awaitIdle(project);

            assertThat(rootsChanges.get()).as("a new content root must fire a roots change").isPositive();
            assertThat(ModuleRootManager.getInstance(module).getContentEntries()).hasSize(2);
        }
        finally {
            Disposer.dispose(disposable);
        }
    }
}
