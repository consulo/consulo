// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.service.project.autoimport;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.impl.internal.service.ExternalSystemProcessingManager;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.importing.ImportSpecBuilder;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.service.project.manage.ExternalProjectsManager;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ProjectAware implements ExternalSystemProjectAware {
    private final Project myProject;
    private final ExternalSystemProjectId myProjectId;
    private final ExternalSystemAutoImportAware myAutoImportAware;

    private final ProjectSystemId mySystemId;
    private final String myExternalProjectPath;

    public ProjectAware(Project project, ExternalSystemProjectId projectId, ExternalSystemAutoImportAware autoImportAware) {
        myProject = project;
        myProjectId = projectId;
        myAutoImportAware = autoImportAware;
        mySystemId = projectId.getSystemId();
        myExternalProjectPath = projectId.getExternalProjectPath();
    }

    @Override
    public ExternalSystemProjectId getProjectId() {
        return myProjectId;
    }

    @Override
    public Set<String> getSettingsFiles() {
        ProjectPathMacroManager pathMacroManager = ProjectPathMacroManager.getInstance(myProject);
        Set<String> result = new LinkedHashSet<>();
        for (Path file : getExternalProjectFiles()) {
            String path = FileUtil.toCanonicalPath(file.toString());
            // The path string can be changed after serialization and deserialization inside persistent component state.
            // To avoid that we resolve the path using IDE path macros configuration.
            String collapsedPath = pathMacroManager.collapsePath(path);
            String expandedPath = pathMacroManager.expandPath(collapsedPath);
            result.add(expandedPath);
        }
        return result;
    }

    private List<Path> getExternalProjectFiles() {
        return myAutoImportAware.getAffectedExternalProjectFilePaths(myExternalProjectPath, myProject);
    }

    @Override
    public void subscribe(ExternalSystemProjectListener listener, Disposable parentDisposable) {
        ExternalSystemProgressNotificationManager progressManager =
            myProject.getApplication().getInstance(ExternalSystemProgressNotificationManager.class);
        TaskNotificationListener taskListener = new TaskNotificationListener(listener);
        progressManager.addNotificationListener(taskListener);
        Disposer.register(parentDisposable, () -> progressManager.removeNotificationListener(taskListener));

        ExternalProjectsManager projectsManager = ExternalProjectsManager.getInstance(myProject);
        projectsManager.runWhenInitialized(listener::onSettingsFilesListChange);
    }

    @Override
    public void reloadProject(ExternalSystemProjectReloadContext context) {
        ImportSpecBuilder importSpec = new ImportSpecBuilder(myProject, mySystemId);
        if (!context.isExplicitReload()) {
            importSpec.dontReportRefreshErrors();
        }
        ExternalSystemUtil.refreshProject(myExternalProjectPath, importSpec);
    }

    private class TaskNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
        private final ExternalSystemProjectListener myDelegate;

        private final AtomicReference<@Nullable ExternalSystemTaskId> myExternalSystemTaskId = new AtomicReference<>(null);

        private TaskNotificationListener(ExternalSystemProjectListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public void onStart(ExternalSystemTaskId id) {
            if (id.getType() != ExternalSystemTaskType.RESOLVE_PROJECT) {
                return;
            }
            if (id.findProject() != myProject) {
                return;
            }

            ExternalSystemProcessingManager processingManager =
                myProject.getApplication().getInstance(ExternalSystemProcessingManager.class);
            ExternalSystemTask task = processingManager.findTask(ExternalSystemTaskType.RESOLVE_PROJECT, mySystemId, myExternalProjectPath);
            if (task == null || !task.getId().equals(id)) {
                return;
            }
            myExternalSystemTaskId.set(id);
            myDelegate.onProjectReloadStart();
        }

        private void afterProjectRefresh(ExternalSystemTaskId id, ExternalSystemRefreshStatus status) {
            if (id.getType() != ExternalSystemTaskType.RESOLVE_PROJECT) {
                return;
            }
            if (!myExternalSystemTaskId.compareAndSet(id, null)) {
                return;
            }
            myDelegate.onProjectReloadFinish(status);
        }

        @Override
        public void onSuccess(ExternalSystemTaskId id) {
            afterProjectRefresh(id, ExternalSystemRefreshStatus.SUCCESS);
        }

        @Override
        public void onFailure(ExternalSystemTaskId id, Exception e) {
            afterProjectRefresh(id, ExternalSystemRefreshStatus.FAILURE);
        }

        @Override
        public void onEnd(ExternalSystemTaskId id) {
            afterProjectRefresh(id, ExternalSystemRefreshStatus.CANCEL);
        }
    }
}
