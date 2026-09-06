// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.index.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposer;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.startup.StartupActivity;
import consulo.util.collection.Lists;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.ManagingFS;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl(id = "ProjectFileBasedIndexStartupActivity", order = "first")
public final class ProjectFileBasedIndexStartupActivity implements StartupActivity.RequiredForSmartMode {
    private final List<Project> myOpenProjects = Lists.newLockFreeCopyOnWriteList();

    @Inject
    public ProjectFileBasedIndexStartupActivity(Application application, Project project) {
        application.getMessageBus().connect(project).subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosing(Project closingProject) {
                if (closingProject == project) {
                    onProjectClosing(closingProject);
                }
            }
        });
    }

    @Override
    public void runActivity(Project project) {
        ProgressManager.progress(IndexingLocalize.progressTextLoadingIndexes());
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        PushedFilePropertiesUpdater propertiesUpdater = PushedFilePropertiesUpdater.getInstance(project);
        if (propertiesUpdater instanceof PushedFilePropertiesUpdaterImpl propertiesUpdaterImpl) {
            propertiesUpdaterImpl.initializeProperties();
        }

        // load indexes while in dumb mode, otherwise someone from read action may hit `FileBasedIndex.getIndex` and hang (IDEA-316697)
        fileBasedIndex.waitUntilIndicesAreInitialized();
        RegisteredIndexes registeredIndexes = fileBasedIndex.getRegisteredIndexes();
        if (registeredIndexes == null) {
            return;
        }

        boolean wasCorrupted = registeredIndexes.getWasCorrupted();

        Path projectQueueFile = PersistentDirtyFilesQueue.getQueueFile(project);
        long vfsCreationTimestamp = ManagingFS.getInstance().getCreationTimestamp();
        ProjectDirtyFilesQueue projectDirtyFilesQueue =
            PersistentDirtyFilesQueue.readProjectDirtyFilesQueue(projectQueueFile, vfsCreationTimestamp);

        // Add a project to various lists in read action to make sure that
        // they are not added to lists during disposing of a project (in this case project may be stuck forever in those lists)
        boolean registered = ReadAction.compute(() -> {
            if (project.isDisposed()) {
                return false;
            }

            // Done mostly for tests.
            // In real life this is no-op, because the set was removed on project closing
            // note that disposing happens in write action, so it'll be executed after this read action
            Disposer.register(project, () -> {
                if (myOpenProjects.remove(project)) {
                    fileBasedIndex.onProjectClosing(project);
                }
            });

            try {
                fileBasedIndex.registerProject(project, projectDirtyFilesQueue.getFileIds());
                fileBasedIndex.registerProjectFileSets(project);
                fileBasedIndex.setLastSeenIndexInOrphanQueue(project, projectDirtyFilesQueue.getLastSeenIndexInOrphanQueue());
                ((ProjectRootManagerComponent) ProjectRootManager.getInstance(project)).projectOpened();

                myOpenProjects.add(project);
                return true;
            }
            catch (Throwable failure) {
                //If any startup step _before_ openProjects.add(project) fails -> automatic project closing won't work.
                // So, for that case we roll back the registration here
                myOpenProjects.remove(project);
                try {
                    fileBasedIndex.onProjectClosing(project);
                }
                catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                ExceptionUtil.rethrow(failure);
                return false;
            }
        });

        if (!registered) {
            return;
        }

        // schedule dumb mode start after the read action we're currently in
        OrphanDirtyFilesQueue orphanQueue = registeredIndexes.getOrphanDirtyFilesQueue();
        CompletableFuture<?> indexCleanupJob = UnindexedFilesScannerStartup.scanAndIndexProjectAfterOpen(
            project,
            orphanQueue,
            registeredIndexes.getOrphanDirtyFilesQueueDiscardReason(),
            fileBasedIndex.getAllDirtyFiles(null),
            projectDirtyFilesQueue,
            !wasCorrupted,
            true,
            "On project open",
            ScanningType.FULL_ON_PROJECT_OPEN,
            ScanningType.PARTIAL_ON_PROJECT_OPEN
        );
        if (indexCleanupJob != null) {
            UnindexedFilesScannerStartup.forgetProjectDirtyFilesOnCompletion(
                indexCleanupJob,
                fileBasedIndex,
                project,
                projectDirtyFilesQueue,
                orphanQueue.getUntrimmedSize()
            );
        }
    }

    private void onProjectClosing(Project project) {
        if (!myOpenProjects.remove(project)) {
            return;
        }

        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        if (!(fileBasedIndex instanceof FileBasedIndexImpl fileBasedIndexImpl)) {
            return;
        }

        ProgressManager.getInstance().executeNonCancelableSection(
            () -> ReadAction.run(() -> fileBasedIndexImpl.onProjectClosing(project))
        );
    }
}
