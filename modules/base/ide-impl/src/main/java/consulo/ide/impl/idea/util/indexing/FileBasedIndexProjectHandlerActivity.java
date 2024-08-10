/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.util.indexing;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.event.ProjectManagerListener;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-08-08
 */
@ExtensionImpl(id = "FileBasedIndexProjectHandlerActivity", order = "first")
public class FileBasedIndexProjectHandlerActivity implements PostStartupActivity, DumbAware {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandlerActivity.class);

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        PushedFilePropertiesUpdater.getInstance(project).initializeProperties();
        FileBasedIndex index = FileBasedIndex.getInstance();

        project.getMessageBus().connect().subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                LOG.info("Has changed files: " + (FileBasedIndexProjectHandler.createChangedFilesIndexingTask(project) != null) + "; project=" + project);
            }
        });

        // schedule dumb mode start after the read action we're currently in
        if (index instanceof FileBasedIndexImpl) {
            DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
        }

        FileBaseIndexSet set = new FileBaseIndexSet(FileBasedIndexScanRunnableCollector.getInstance(project));
        index.registerIndexableSet(set, project);

        Disposable listener = Disposable.newDisposable("project close listener");
        Disposer.register(project, listener);

        project.getMessageBus().connect(listener).subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            private boolean removed;

            @Override
            public void projectClosing(@Nonnull Project eventProject) {
                if (eventProject == project && !removed) {
                    removed = true;
                    index.removeIndexableSet(set);
                }
            }
        });
    }
}
