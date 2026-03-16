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
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.ProjectCloseService;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.coroutine.UIAction;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
@ServiceImpl
@Singleton
public class ProjectCloseServiceImpl implements ProjectCloseService {
    private final Application myApplication;
    private final ProgressBuilderFactory myProgressBuilderFactory;

    @Inject
    public ProjectCloseServiceImpl(Application application,
                                   ProgressBuilderFactory progressBuilderFactory) {
        myApplication = application;
        myProgressBuilderFactory = progressBuilderFactory;
    }

    @Override
    public CompletableFuture<Boolean> closeProjectAsync(
        Project project,
        UIAccess uiAccess,
        boolean checkCanClose,
        boolean save,
        boolean dispose
    ) {
        ProjectManagerImpl manager = (ProjectManagerImpl) ProjectManager.getInstance();

        if (!manager.isProjectOpened(project)) {
            return CompletableFuture.completedFuture(true);
        }

        return myProgressBuilderFactory
            .newProgressBuilder(project, ProjectLocalize.projectCloseProgress())
            .execute(uiAccess, () -> {
                // Step 1 (UIAction): Check canClose veto (may show dialogs)
                Coroutine<?, Boolean> chain = Coroutine
                    .first(UIAction.<Boolean, Boolean>apply((input, continuation) -> {
                        if (checkCanClose && !manager.canClose(project)) {
                            @SuppressWarnings("unchecked")
                            Continuation<Boolean> typed = (Continuation<Boolean>) continuation;
                            typed.finishEarly(Boolean.FALSE);
                            return null;
                        }
                        return Boolean.TRUE;
                    }));

                // Step 2 (WriteLock): Save all documents (requires write action)
                if (save) {
                    chain = chain.then(WriteLock.<Boolean, Boolean>apply(proceed -> {
                        FileDocumentManager.getInstance().saveAllDocuments(uiAccess);
                        return proceed;
                    }));

                    // Step 2b (CodeExecution): Save project state (no lock required)
                    chain = chain.then(CodeExecution.<Boolean, Boolean>apply(proceed -> {
                        project.save(uiAccess);
                        return proceed;
                    }));
                }

                // Step 3 (UIAction): Check ensureCouldCloseIfUnableToSave
                if (checkCanClose) {
                    chain = chain.then(UIAction.<Boolean, Boolean>apply((proceed, continuation) -> {
                        if (!ensureCouldCloseIfUnableToSave(project)) {
                            @SuppressWarnings("unchecked")
                            Continuation<Boolean> typed = (Continuation<Boolean>) continuation;
                            typed.finishEarly(Boolean.FALSE);
                            return null;
                        }
                        return proceed;
                    }));
                }

                // Step 4 (CodeExecution): Fire projectClosing event
                // NOT in write action -- allows progress bars from listeners
                chain = chain.then(CodeExecution.<Boolean, Boolean>apply(proceed -> {
                    myApplication.getMessageBus()
                        .syncPublisher(ProjectManagerListener.class)
                        .projectClosing(project);
                    return proceed;
                }));

                // Step 5 (WriteLock): removeFromOpened + fire projectClosed
                chain = chain.then(WriteLock.<Boolean, Boolean>apply((proceed, continuation) -> {
                    Thread currentThread = Thread.currentThread();
                    ShutDownTracker.getInstance().registerStopperThread(currentThread);
                    try {
                        // Guard against concurrent close
                        if (!manager.isProjectOpened(project)) {
                            return Boolean.TRUE;
                        }

                        manager.removeFromOpened(project);

                        myApplication.getMessageBus()
                            .syncPublisher(ProjectManagerListener.class)
                            .projectClosed(project, uiAccess);
                    }
                    finally {
                        ShutDownTracker.getInstance().unregisterStopperThread(currentThread);
                    }
                    return Boolean.TRUE;
                }));

                return chain;
            }).thenCompose(closed -> {
                // Dispose AFTER progress completes — disposing inside the progress
                // would cancel the project's coroutine scope and kill the progress itself.
                // Must run under write lock, and callers must wait for dispose to finish.
                if (dispose && Boolean.TRUE.equals(closed)) {
                    CompletableFuture<Boolean> disposeFuture = new CompletableFuture<>();
                    WriteAction.runLater(() -> {
                        Disposer.dispose(project);
                        disposeFuture.complete(Boolean.TRUE);
                    });
                    return disposeFuture;
                }
                return CompletableFuture.completedFuture(closed);
            });
    }

    @RequiredUIAccess
    private static boolean ensureCouldCloseIfUnableToSave(Project project) {
        ProjectStorageUtil.UnableToSaveProjectNotification[] notifications =
            NotificationsManager.getNotificationsManager()
                .getNotificationsOfType(ProjectStorageUtil.UnableToSaveProjectNotification.class, project);

        if (notifications.length == 0) {
            return true;
        }

        String fileNames = StringUtil.join(notifications[0].getFileNames(), "\n");

        return Messages.showDialog(
            project,
            ProjectLocalize.dialogUnsavedProjectText(project.getApplication().getName()).get(),
            ProjectLocalize.dialogUnsavedProjectTitle().get(),
            ProjectLocalize.dialogUnsavedProjectReadOnlyFiles(fileNames).get(),
            new String[]{CommonLocalize.buttonYes().get(), CommonLocalize.buttonNo().get()},
            0,
            1,
            UIUtil.getWarningIcon()
        ) == 0;
    }
}
