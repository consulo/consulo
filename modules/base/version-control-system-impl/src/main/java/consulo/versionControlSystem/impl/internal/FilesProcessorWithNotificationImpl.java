// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class FilesProcessorWithNotificationImpl extends FilesProcessorImpl {
    private final Object NOTIFICATION_LOCK = new Object();

    private @Nullable Notification myNotification;

    protected FilesProcessorWithNotificationImpl(Project project, Disposable parentDisposable) {
        super(project, parentDisposable);
    }

    protected abstract String getAskedBeforeProperty();

    protected abstract String getDoForCurrentProjectProperty();

    protected abstract LocalizeValue getShowActionText();

    protected abstract LocalizeValue getForCurrentProjectActionText();

    protected LocalizeValue getForAllProjectsActionText() {
        return LocalizeValue.empty();
    }

    protected abstract LocalizeValue getMuteActionText();

    protected abstract LocalizeValue notificationTitle();

    protected abstract LocalizeValue notificationMessage();

    protected LocalizeValue getViewFilesDialogTitle() {
        return LocalizeValue.empty();
    }

    protected LocalizeValue getViewFilesDialogOkActionName() {
        return CommonLocalize.buttonAdd();
    }

    protected LocalizeValue getViewFilesDialogCancelActionName() {
        return CommonLocalize.buttonCancel();
    }

    @Override
    protected void handleProcessingForCurrentProject() {
        synchronized (NOTIFICATION_LOCK) {
            if (notAskedBefore() && notificationNotPresent()) {
                // propose to process files
                Notification.Builder builder = NotificationService.getInstance()
                    .newInfo(VcsNotifier.STANDARD_NOTIFICATION)
                    .title(notificationTitle())
                    .content(notificationMessage())
                    .addAction(getShowActionText(), this::showFiles)
                    .addClosingAction(getForCurrentProjectActionText(), () -> {
                        doActionOnChosenFiles(acquireValidFiles());
                        setForCurrentProject(true);
                        ProjectPropertiesComponent.getInstance(myProject).setValue(getAskedBeforeProperty(), true);
                    });

                LocalizeValue forAllProjectsActionText = getForAllProjectsActionText();
                if (forAllProjectsActionText.isNotEmpty()) {
                    builder.addClosingAction(forAllProjectsActionText, () -> {
                        doActionOnChosenFiles(acquireValidFiles());
                        setForCurrentProject(true);
                        ProjectPropertiesComponent.getInstance(myProject).setValue(getAskedBeforeProperty(), true);
                        rememberForAllProjects();
                    });
                }

                builder.addClosingAction(getMuteActionText(), () -> {
                    setForCurrentProject(false);
                    ProjectPropertiesComponent.getInstance(myProject).setValue(getAskedBeforeProperty(), true);
                });

                myNotification = builder.notifyAndGet(myProject);
            }
        }
    }

    @RequiredUIAccess
    private void showFiles() {
        List<VirtualFile> allFiles = selectValidFiles();
        if (allFiles.isEmpty()) {
            return;
        }

        myProject.getApplication().getInstance(VcsChangesDialogFactory.class)
            .selectFiles(myProject, allFiles)
            .title(getViewFilesDialogTitle())
            .selectable()
            .deletable()
            .okText(getViewFilesDialogOkActionName())
            .cancelText(getViewFilesDialogCancelActionName())
            .showAsync()
            .whenCompleteAsync((selected, throwable) -> {
                if (selected == null || selected.isEmpty()) {
                    return;
                }
                List<VirtualFile> userSelectedFiles = selected.get();
                doActionOnChosenFiles(userSelectedFiles);
                removeFiles(userSelectedFiles);
                if (isFilesEmpty()) {
                    expireNotification();
                }
            }, myProject.getUIAccess());
    }

    private boolean notificationNotPresent() {
        synchronized (NOTIFICATION_LOCK) {
            return myNotification == null || myNotification.isExpired();
        }
    }

    private void expireNotification() {
        synchronized (NOTIFICATION_LOCK) {
            if (myNotification != null) {
                myNotification.expire();
            }
        }
    }

    protected void rememberForAllProjects() {
        throw new UnsupportedOperationException();
    }

    protected void setForCurrentProject(boolean isEnabled) {
        ProjectPropertiesComponent.getInstance(myProject).setValue(getDoForCurrentProjectProperty(), isEnabled);
    }

    @Override
    protected boolean needDoForCurrentProject() {
        return ProjectPropertiesComponent.getInstance(myProject).getBoolean(getDoForCurrentProjectProperty(), false);
    }

    private boolean notAskedBefore() {
        return !wasAskedBefore();
    }

    private boolean wasAskedBefore() {
        return ProjectPropertiesComponent.getInstance(myProject).getBoolean(getAskedBeforeProperty(), false);
    }
}
