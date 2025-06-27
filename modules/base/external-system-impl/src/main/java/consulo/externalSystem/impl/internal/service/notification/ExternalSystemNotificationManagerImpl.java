package consulo.externalSystem.impl.internal.service.notification;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.externalSystem.ExternalSystemConfigurableAware;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.internal.ExternalSystemInternalNotificationHelper;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.rt.model.LocationAwareExternalSystemException;
import consulo.externalSystem.service.notification.*;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileEditor.EditorNotifications;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.rmi.RemoteUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * This class is responsible for ide user by external system integration-specific events.
 * <p/>
 * One example use-case is a situation when an error occurs during external project refresh. We need to
 * show corresponding message to the end-user.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov, Vladislav Soroka
 * @since 2012-03-21
 */
@Singleton
@ServiceImpl
public class ExternalSystemNotificationManagerImpl implements ExternalSystemNotificationManager {

    @Nonnull
    private final ExecutorService myUpdater =
        SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ExternalSystemNotificationManager pool");

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ExternalSystemInternalNotificationHelper myNotificationHelper;
    @Nonnull
    private final NotificationService myNotificationService;
    @Nonnull
    private final List<Notification> myNotifications = new ArrayList<>();
    @Nonnull
    private final Set<ProjectSystemId> initializedExternalSystem = new HashSet<>();
    @Nonnull
    private final MessageCounter myMessageCounter = new MessageCounter();

    @Inject
    public ExternalSystemNotificationManagerImpl(
        @Nonnull Project project,
        @Nonnull ExternalSystemInternalNotificationHelper notificationHelper,
        @Nonnull NotificationService notificationService
    ) {
        myProject = project;
        myNotificationHelper = notificationHelper;
        myNotificationService = notificationService;
    }

    @Override
    public void processExternalProjectRefreshError(
        @Nonnull Throwable error,
        @Nonnull String externalProjectName,
        @Nonnull ProjectSystemId externalSystemId
    ) {
        if (myProject.isDisposed() || !myProject.isOpen()) {
            return;
        }
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        if (!(manager instanceof ExternalSystemConfigurableAware)) {
            return;
        }

        LocalizeValue title = ExternalSystemLocalize.notificationProjectRefreshFailTitle(
            externalSystemId.getReadableName(),
            externalProjectName
        );
        String message = ExternalSystemApiUtil.buildErrorMessage(error);
        NotificationCategory notificationCategory = NotificationCategory.ERROR;
        String filePath = null;
        Integer line = null;
        Integer column = null;

        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable unwrapped = RemoteUtil.unwrap(error);
        if (unwrapped instanceof LocationAwareExternalSystemException locationAwareExternalSystemException) {
            filePath = locationAwareExternalSystemException.getFilePath();
            line = locationAwareExternalSystemException.getLine();
            column = locationAwareExternalSystemException.getColumn();
        }

        NotificationData notificationData = new NotificationData(
            title.get(),
            message,
            notificationCategory,
            NotificationSource.PROJECT_SYNC,
            filePath,
            ObjectUtil.notNull(line, -1),
            ObjectUtil.notNull(column, -1),
            false
        );

        for (ExternalSystemNotificationExtension extension :
            myProject.getApplication().getExtensionList(ExternalSystemNotificationExtension.class)) {
            if (!externalSystemId.equals(extension.getTargetExternalSystemId())) {
                continue;
            }
            extension.customize(notificationData, myProject, error);
        }

        EditorNotifications.getInstance(myProject).updateAllNotifications();
        showNotification(externalSystemId, notificationData);
    }

    public void showNotification(@Nonnull ProjectSystemId externalSystemId, @Nonnull NotificationData notificationData) {
        myUpdater.execute(() -> {
            if (myProject.isDisposed()) {
                return;
            }

            if (!initializedExternalSystem.contains(externalSystemId)) {
                Application app = myProject.getApplication();
                Runnable action = () -> app.runWriteAction(() -> {
                    if (myProject.isDisposed()) {
                        return;
                    }
                    ExternalSystemUtil.ensureToolWindowContentInitialized(myProject, externalSystemId);
                    initializedExternalSystem.add(externalSystemId);
                });
                if (app.isDispatchThread()) {
                    action.run();
                }
                else {
                    app.invokeAndWait(action, app.getDefaultModalityState());
                }
            }

            NotificationGroup group = ExternalSystemUtil.getToolWindowElement(
                NotificationGroup.class, myProject, ExternalSystemDataKeys.NOTIFICATION_GROUP, externalSystemId);
            if (group == null) {
                return;
            }

            Notification notification = myNotificationService
                .newOfType(group, notificationData.getNotificationCategory().getNotificationType())
                .title(LocalizeValue.localizeTODO(notificationData.getTitle()))
                .content(LocalizeValue.localizeTODO(notificationData.getMessage()))
                .hyperlinkListener(notificationData.getListener())
                .create();

            myNotifications.add(notification);

            if (notificationData.isBalloonNotification()) {
                applyNotification(notification);
            }
            else {
                addMessage(notification, externalSystemId, notificationData);
            }
        });
    }

    @RequiredUIAccess
    private void addMessage(
        @Nonnull Notification notification,
        @Nonnull ProjectSystemId externalSystemId,
        @Nonnull NotificationData notificationData
    ) {
        VirtualFile virtualFile =
            notificationData.getFilePath() != null ? ExternalSystemUtil.waitForTheFile(notificationData.getFilePath()) : null;
        String groupName = virtualFile != null ? virtualFile.getPresentableUrl() : notificationData.getTitle();

        myMessageCounter
            .increment(groupName, notificationData.getNotificationSource(), notificationData.getNotificationCategory(), externalSystemId);

        myNotificationHelper.addMessage(virtualFile, groupName, notification, externalSystemId, notificationData);
    }

    @Override
    public void clearNotifications(
        @Nonnull NotificationSource notificationSource,
        @Nonnull ProjectSystemId externalSystemId
    ) {
        @Deprecated
        String groupName = null;
        myMessageCounter.remove(groupName, notificationSource, externalSystemId);
        myUpdater.execute(() -> {
            for (Iterator<Notification> iterator = myNotifications.iterator(); iterator.hasNext(); ) {
                Notification notification = iterator.next();
                notification.expire();
                iterator.remove();
            }

            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
            if (toolWindow == null) {
                return;
            }

            Pair<NotificationSource, ProjectSystemId> contentIdPair = Pair.create(notificationSource, externalSystemId);
            MessageView messageView = myProject.getInstance(MessageView.class);
            UIUtil.invokeLaterIfNeeded(() -> {
                for (Content content : messageView.getContentManager().getContents()) {
                    if (!content.isPinned() && contentIdPair.equals(content.getUserData(ExternalSystemInternalNotificationHelper.CONTENT_ID_KEY))) {
                        messageView.getContentManager().removeContent(content, true);
                    }
                }
            });
        });
    }

    public int getMessageCount(
        @Nonnull NotificationSource notificationSource,
        @Nullable NotificationCategory notificationCategory,
        @Nonnull ProjectSystemId externalSystemId
    ) {
        return getMessageCount(null, notificationSource, notificationCategory, externalSystemId);
    }

    public int getMessageCount(
        @Nullable String groupName,
        @Nonnull NotificationSource notificationSource,
        @Nullable NotificationCategory notificationCategory,
        @Nonnull ProjectSystemId externalSystemId
    ) {
        return myMessageCounter.getCount(groupName, notificationSource, notificationCategory, externalSystemId);
    }

    private void applyNotification(@Nonnull Notification notification) {
        if (!myProject.isDisposed() && myProject.isOpen()) {
            notification.notify(myProject);
        }
    }
}