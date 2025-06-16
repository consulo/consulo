/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.ReadTask;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.impl.internal.EditorNotificationBuilderFactory;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.AsyncEditorLoader;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class EditorNotificationsImpl extends EditorNotifications implements Disposable {
    private record NotificationInfo(EditorNotificationBuilder builder, Disposable disposer) {
    }

    private static final Key<WeakReference<ProgressIndicator>> CURRENT_UPDATES = Key.create("CURRENT_UPDATES");
    private final ExecutorService myExecutor;

    private final Project myProject;
    private final FileEditorManager myFileEditorManager;
    private final EditorNotificationBuilderFactory myEditorNotificationBuilderFactory;

    private final Map<String, Key<NotificationInfo>> myKeyStore = new ConcurrentHashMap<>();

    private Future<?> myUpdateAllFuture = CompletableFuture.completedFuture(null);

    @Inject
    public EditorNotificationsImpl(
        ApplicationConcurrency applicationConcurrency,
        Project project,
        FileEditorManager fileEditorManager,
        EditorNotificationBuilderFactory notificationBuilderFactory
    ) {
        myProject = project;
        myExecutor = applicationConcurrency.createSequentialApplicationPoolExecutor("EditorNotificationsImpl pool");
        myEditorNotificationBuilderFactory = notificationBuilderFactory;
        myFileEditorManager = fileEditorManager;
        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
                updateNotifications(file);
            }
        });
        connection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                updateAllNotifications();
            }

            @Override
            public void exitDumbMode() {
                updateAllNotifications();
            }
        });
        connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                updateAllNotifications();
            }
        });
    }

    @Override
    public void updateNotifications(@Nonnull VirtualFile file) {
        myProject.getApplication().getLastUIAccess().giveIfNeed(() -> {
            ProgressIndicator indicator = getCurrentProgress(file);
            if (indicator != null) {
                indicator.cancel();
            }
            file.putUserData(CURRENT_UPDATES, null);

            if (myProject.isDisposed() || !file.isValid()) {
                return;
            }

            indicator = new ProgressIndicatorBase();
            ReadTask task = createTask(indicator, file);
            if (task == null) {
                return;
            }

            file.putUserData(CURRENT_UPDATES, new WeakReference<>(indicator));

            ProgressIndicatorUtils.scheduleWithWriteActionPriority(indicator, myExecutor, task);
        });
    }

    @Nullable
    private ReadTask createTask(@Nonnull ProgressIndicator indicator, @Nonnull VirtualFile file) {
        List<FileEditor> editors = ContainerUtil.filter(
            myFileEditorManager.getAllEditors(file),
            editor -> !(editor instanceof TextEditor textEditor && !AsyncEditorLoader.isEditorLoaded(textEditor.getEditor()))
        );

        if (editors.isEmpty()) {
            return null;
        }

        return new ReadTask() {
            private boolean isOutdated() {
                if (myProject.isDisposed() || !file.isValid() || indicator != getCurrentProgress(file)) {
                    return true;
                }

                for (FileEditor editor : editors) {
                    if (!editor.isValid()) {
                        return true;
                    }
                }

                return false;
            }

            @RequiredReadAction
            @Nullable
            @Override
            public Continuation performInReadAction(@Nonnull ProgressIndicator indicator) throws ProcessCanceledException {
                if (isOutdated()) {
                    return null;
                }

                List<EditorNotificationProvider> providers =
                    DumbService.getInstance(myProject).filterByDumbAwareness(myProject.getExtensionList(EditorNotificationProvider.class));

                List<Runnable> updates = new ArrayList<>();
                for (FileEditor editor : editors) {
                    for (EditorNotificationProvider provider : providers) {
                        EditorNotificationBuilder builder =
                            provider.buildNotification(file, editor, myEditorNotificationBuilderFactory::newBuilder);
                        updates.add(() -> updateNotification(editor, provider.getId(), (EditorNotificationBuilderEx)builder));
                    }
                }

                return new Continuation(() -> {
                    if (!isOutdated()) {
                        file.putUserData(CURRENT_UPDATES, null);
                        for (Runnable update : updates) {
                            update.run();
                        }
                    }
                }, myProject.getApplication().getAnyModalityState());
            }

            @Override
            public void onCanceled(@Nonnull ProgressIndicator ignored) {
                if (getCurrentProgress(file) == indicator) {
                    updateNotifications(file);
                }
            }
        };
    }

    private static ProgressIndicator getCurrentProgress(VirtualFile file) {
        return SoftReference.dereference(file.getUserData(CURRENT_UPDATES));
    }

    private void updateNotification(
        @Nonnull FileEditor editor,
        @Nonnull String notificationId,
        @Nullable EditorNotificationBuilderEx builder
    ) {
        Key<NotificationInfo> key = myKeyStore.computeIfAbsent(notificationId, Key::create);

        NotificationInfo oldData = editor.getUserData(key);
        if (oldData != null) {
            oldData.disposer().dispose();
            // reset value
            editor.putUserData(key, null);
        }

        if (builder != null) {
            Disposable disposer = myFileEditorManager.addTopComponent(editor, builder);
            if (disposer != null) {
                editor.putUserData(key, new NotificationInfo(builder, disposer));
            }
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void updateAllNotifications() {
        myUpdateAllFuture.cancel(false);
        myUpdateAllFuture = myProject.getUIAccess().getScheduler().schedule(() -> {
            for (VirtualFile file : myFileEditorManager.getOpenFiles()) {
                updateNotifications(file);
            }
        }, 100, TimeUnit.MILLISECONDS);
    }
}
