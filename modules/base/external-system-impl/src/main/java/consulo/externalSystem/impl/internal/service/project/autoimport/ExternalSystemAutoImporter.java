/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.project.autoimport;

import consulo.codeEditor.EditorFactory;
import consulo.component.messagebus.MessageBus;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.service.ExternalSystemProcessingManager;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskState;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Denis Zhdanov
 * @since 2013-06-07
 */
public class ExternalSystemAutoImporter implements BulkFileListener, DocumentListener {
    @Nonnull
    private final ConcurrentMap<ProjectSystemId, Set<String /* external project path */>> myFilesToRefresh =
        ContainerUtil.newConcurrentMap();

    @Nonnull
    private final Alarm myVfsAlarm;
    @Nonnull
    private final ReadWriteLock myVfsLock = new ReentrantReadWriteLock();

    @Nonnull
    private final Set<Document> myDocumentsToSave = new HashSet<>();
    @Nonnull
    private final Alarm myDocumentAlarm;
    @Nonnull
    private final ReadWriteLock myDocumentLock = new ReentrantReadWriteLock();

    @Nonnull
    private final Runnable myFilesRequest = this::refreshFilesIfNecessary;
    @Nonnull
    private final Runnable myDocumentsSaveRequest = this::saveDocumentsIfNecessary;
    @Nonnull
    private final ExternalProjectRefreshCallback myRefreshCallback = new ExternalProjectRefreshCallback() {
        @Override
        @RequiredUIAccess
        public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
            if (externalProject != null) {
                ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(myProject) {
                    @RequiredUIAccess
                    @Override
                    public void execute() {
                        ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(() -> myProjectDataManager.importData(
                            externalProject.getKey(),
                            Collections.singleton(externalProject),
                            myProject,
                            true
                        ));
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
            // Do nothing.
        }
    };

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ProjectDataManager myProjectDataManager;

    @Nonnull
    private final MyEntry[] myAutoImportAware;

    public ExternalSystemAutoImporter(
        @Nonnull Project project,
        @Nonnull ProjectDataManager projectDataManager,
        @Nonnull MyEntry[] autoImportAware
    ) {
        myProject = project;
        myProjectDataManager = projectDataManager;
        myAutoImportAware = autoImportAware;
        myDocumentAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
        myVfsAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
    }

    @SuppressWarnings("unchecked")
    public static void letTheMagicBegin(@Nonnull Project project) {
        List<MyEntry> autoImportAware = new ArrayList<>();
        project.getApplication().getExtensionPoint(ExternalSystemManager.class).forEach(manager -> {
            AbstractExternalSystemSettings<?, ?, ?> systemSettings =
                ((ExternalSystemManager<?, ?, ?, ?, ?>)manager).getSettingsProvider().apply(project);
            ExternalSystemAutoImportAware defaultImportAware = createDefault(systemSettings);
            ExternalSystemAutoImportAware aware = manager instanceof ExternalSystemAutoImportAware externalSystemAutoImportAware
                ? combine(defaultImportAware, externalSystemAutoImportAware)
                : defaultImportAware;
            autoImportAware.add(new MyEntry(manager.getSystemId(), systemSettings, aware));
        });

        MyEntry[] entries = autoImportAware.toArray(new MyEntry[autoImportAware.size()]);
        ExternalSystemAutoImporter autoImporter =
            new ExternalSystemAutoImporter(project, project.getApplication().getInstance(ProjectDataManager.class), entries);
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(BulkFileListener.class, autoImporter);

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(autoImporter, project);
    }

    @Nonnull
    private static ExternalSystemAutoImportAware combine(
        @Nonnull ExternalSystemAutoImportAware aware1,
        @Nonnull ExternalSystemAutoImportAware aware2
    ) {
        return new ExternalSystemAutoImportAware() {
            @Nullable
            @Override
            public String getAffectedExternalProjectPath(@Nonnull String changedFileOrDirPath, @Nonnull Project project) {
                String projectPath = aware1.getAffectedExternalProjectPath(changedFileOrDirPath, project);
                return projectPath == null ? aware2.getAffectedExternalProjectPath(changedFileOrDirPath, project) : projectPath;
            }
        };
    }

    @Nonnull
    private static ExternalSystemAutoImportAware createDefault(@Nonnull AbstractExternalSystemSettings<?, ?, ?> systemSettings) {
        return new ExternalSystemAutoImportAware() {
            @Nullable
            @Override
            public String getAffectedExternalProjectPath(@Nonnull String changedFileOrDirPath, @Nonnull Project project) {
                return systemSettings.getLinkedProjectSettings(changedFileOrDirPath) == null ? null : changedFileOrDirPath;
            }
        };
    }

    @Override
    public void beforeDocumentChange(DocumentEvent event) {
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile file = fileDocumentManager.getFile(document);
        if (file == null) {
            return;
        }

        String path = ExternalSystemApiUtil.getLocalFileSystemPath(file);
        for (MyEntry entry : myAutoImportAware) {
            if (entry.aware.getAffectedExternalProjectPath(path, myProject) != null) {
                // Document save triggers VFS event but FileDocumentManager might be registered after the current listener, that's why
                // call to 'saveDocument()' might not produce the desired effect. That's why we reschedule document save if necessary.
                scheduleDocumentSave(document);
                return;
            }
        }
    }

    private void scheduleDocumentSave(@Nonnull Document document) {
        Lock lock = myDocumentLock.readLock();
        lock.lock();
        try {
            myDocumentsToSave.add(document);
            if (myDocumentAlarm.getActiveRequestCount() <= 0) {
                myDocumentAlarm.addRequest(myDocumentsSaveRequest, 100);
            }
        }
        finally {
            lock.unlock();
        }
    }

    private void saveDocumentsIfNecessary() {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Lock lock = myDocumentLock.writeLock();
        Set<Document> toKeep = new HashSet<>();
        Set<Document> toSave = new HashSet<>();
        lock.lock();
        try {
            myDocumentAlarm.cancelAllRequests();
            for (Document document : myDocumentsToSave) {
                if (fileDocumentManager.isDocumentUnsaved(document)) {
                    toSave.add(document);
                }
                else {
                    toKeep.add(document);
                }
            }
            myDocumentsToSave.clear();
            if (!toSave.isEmpty()) {
                UIUtil.invokeLaterIfNeeded(() -> {
                    for (Document document : toSave) {
                        fileDocumentManager.saveDocument(document);
                    }
                });
            }
            if (!toKeep.isEmpty()) {
                myDocumentsToSave.addAll(toKeep);
                myDocumentAlarm.addRequest(myDocumentsSaveRequest, 100);
            }
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void before(@Nonnull List<? extends VFileEvent> events) {
    }

    @Override
    public void after(@Nonnull List<? extends VFileEvent> events) {
        boolean scheduleRefresh = false;
        for (VFileEvent event : events) {
            String changedPath = event.getPath();
            for (MyEntry entry : myAutoImportAware) {
                String projectPath = entry.aware.getAffectedExternalProjectPath(changedPath, myProject);
                if (projectPath == null) {
                    continue;
                }
                ExternalProjectSettings projectSettings = entry.systemSettings.getLinkedProjectSettings(projectPath);
                if (projectSettings != null && projectSettings.isUseAutoImport()) {
                    addPath(entry.externalSystemId, projectPath);
                    scheduleRefresh = true;
                    break;
                }
            }
        }
        if (scheduleRefresh) {
            myVfsAlarm.cancelAllRequests();
            myVfsAlarm.addRequest(myFilesRequest, ExternalSystemConstants.AUTO_IMPORT_DELAY_MILLIS);
        }
    }

    private void addPath(@Nonnull ProjectSystemId externalSystemId, @Nonnull String path) {
        Lock lock = myVfsLock.readLock();
        lock.lock();
        try {
            Set<String> paths = myFilesToRefresh.get(externalSystemId);
            while (paths == null) {
                myFilesToRefresh.putIfAbsent(externalSystemId, new HashSet<>());
                paths = myFilesToRefresh.get(externalSystemId);
            }
            paths.add(path);
        }
        finally {
            lock.unlock();
        }
    }

    private void refreshFilesIfNecessary() {
        if (myFilesToRefresh.isEmpty() || myProject.isDisposed()) {
            return;
        }

        Map<ProjectSystemId, Set<String>> copy = new HashMap<>();
        Lock fileLock = myVfsLock.writeLock();
        fileLock.lock();
        try {
            copy.putAll(myFilesToRefresh);
            myFilesToRefresh.clear();
        }
        finally {
            fileLock.unlock();
        }

        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        Lock documentLock = myDocumentLock.writeLock();
        documentLock.lock();
        try {
            for (Set<String> paths : copy.values()) {
                for (String path : paths) {
                    VirtualFile file = fileSystem.findFileByPath(path);
                    if (file != null) {
                        Document document = fileDocumentManager.getCachedDocument(file);
                        if (document != null) {
                            myDocumentsToSave.remove(document);
                        }
                    }
                }
            }
        }
        finally {
            documentLock.unlock();
        }

        boolean scheduleRefresh = false;
        ExternalSystemProcessingManager processingManager = myProject.getApplication().getInstance(ExternalSystemProcessingManager.class);
        for (Map.Entry<ProjectSystemId, Set<String>> entry : copy.entrySet()) {
            for (String path : entry.getValue()) {
                ExternalSystemTask resolveTask = processingManager.findTask(ExternalSystemTaskType.RESOLVE_PROJECT, entry.getKey(), path);
                ExternalSystemTaskState taskState = resolveTask == null ? null : resolveTask.getState();
                if (taskState == null || taskState.isStopped()
                    || (taskState == ExternalSystemTaskState.IN_PROGRESS && resolveTask.cancel())) {
                    ExternalSystemUtil.refreshProject(
                        myProject,
                        entry.getKey(),
                        path,
                        myRefreshCallback,
                        false,
                        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                        false
                    );
                }
                else if (taskState != ExternalSystemTaskState.NOT_STARTED) {
                    // re-schedule to wait for the project import task end
                    scheduleRefresh = true;
                    addPath(entry.getKey(), path);
                }
            }
        }

        if (scheduleRefresh) {
            myVfsAlarm.cancelAllRequests();
            myVfsAlarm.addRequest(myFilesRequest, ExternalSystemConstants.AUTO_IMPORT_DELAY_MILLIS);
        }
    }

    private static class MyEntry {

        @Nonnull
        public final ProjectSystemId externalSystemId;
        @Nonnull
        public final AbstractExternalSystemSettings<?, ?, ?> systemSettings;
        @Nonnull
        public final ExternalSystemAutoImportAware aware;

        MyEntry(
            @Nonnull ProjectSystemId externalSystemId,
            @Nonnull AbstractExternalSystemSettings<?, ?, ?> systemSettings,
            @Nonnull ExternalSystemAutoImportAware aware
        ) {
            this.externalSystemId = externalSystemId;
            this.systemSettings = systemSettings;
            this.aware = aware;
        }
    }
}
