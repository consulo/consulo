package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.execution.rmi.RemoteUtil;
import com.intellij.ide.errorTreeView.*;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.MessageView;
import com.intellij.util.ObjectUtil;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
 * @since 3/21/12 4:04 PM
 */
@Singleton
public class ExternalSystemNotificationManager {
  @Nonnull
  private static final Key<Pair<NotificationSource, ProjectSystemId>> CONTENT_ID_KEY = Key.create("CONTENT_ID");

  @Nonnull
  private final ExecutorService myUpdater = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ExternalSystemNotificationManager pool");

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final List<Notification> myNotifications;
  @Nonnull
  private final Set<ProjectSystemId> initializedExternalSystem;
  @Nonnull
  private final MessageCounter myMessageCounter;

  @Inject
  public ExternalSystemNotificationManager(@Nonnull final Project project) {
    myProject = project;
    myNotifications = ContainerUtil.newArrayList();
    initializedExternalSystem = ContainerUtil.newHashSet();
    myMessageCounter = new MessageCounter();
  }

  @Nonnull
  public static ExternalSystemNotificationManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ExternalSystemNotificationManager.class);
  }

  public void processExternalProjectRefreshError(@Nonnull Throwable error,
                                                 @Nonnull String externalProjectName,
                                                 @Nonnull ProjectSystemId externalSystemId) {
    if (myProject.isDisposed() || !myProject.isOpen()) {
      return;
    }
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    if (!(manager instanceof ExternalSystemConfigurableAware)) {
      return;
    }

    String title =
            ExternalSystemBundle.message("notification.project.refresh.fail.title", externalSystemId.getReadableName(), externalProjectName);
    String message = ExternalSystemApiUtil.buildErrorMessage(error);
    NotificationCategory notificationCategory = NotificationCategory.ERROR;
    String filePath = null;
    Integer line = null;
    Integer column = null;

    //noinspection ThrowableResultOfMethodCallIgnored
    Throwable unwrapped = RemoteUtil.unwrap(error);
    if (unwrapped instanceof LocationAwareExternalSystemException) {
      LocationAwareExternalSystemException locationAwareExternalSystemException = (LocationAwareExternalSystemException)unwrapped;
      filePath = locationAwareExternalSystemException.getFilePath();
      line = locationAwareExternalSystemException.getLine();
      column = locationAwareExternalSystemException.getColumn();
    }

    NotificationData notificationData =
            new NotificationData(
                    title, message, notificationCategory, NotificationSource.PROJECT_SYNC,
                    filePath, ObjectUtil.notNull(line, -1), ObjectUtil.notNull(column, -1), false);

    for (ExternalSystemNotificationExtension extension : ExternalSystemNotificationExtension.EP_NAME.getExtensions()) {
      if (!externalSystemId.equals(extension.getTargetExternalSystemId())) {
        continue;
      }
      extension.customize(notificationData, myProject, error);
    }

    EditorNotifications.getInstance(myProject).updateAllNotifications();
    showNotification(externalSystemId, notificationData);
  }

  public void showNotification(@Nonnull final ProjectSystemId externalSystemId, @Nonnull final NotificationData notificationData) {
    myUpdater.execute(new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;

        if (!initializedExternalSystem.contains(externalSystemId)) {
          final Application app = ApplicationManager.getApplication();
          Runnable action = new Runnable() {
            @Override
            public void run() {
              app.runWriteAction(new Runnable() {
                @Override
                public void run() {
                  if (myProject.isDisposed()) return;
                  ExternalSystemUtil.ensureToolWindowContentInitialized(myProject, externalSystemId);
                  initializedExternalSystem.add(externalSystemId);
                }
              });
            }
          };
          if (app.isDispatchThread()) {
            action.run();
          }
          else {
            app.invokeAndWait(action, ModalityState.defaultModalityState());
          }
        }

        final NotificationGroup group = ExternalSystemUtil.getToolWindowElement(
                NotificationGroup.class, myProject, ExternalSystemDataKeys.NOTIFICATION_GROUP, externalSystemId);
        if (group == null) return;

        final Notification notification = group.createNotification(
                notificationData.getTitle(), notificationData.getMessage(),
                notificationData.getNotificationCategory().getNotificationType(), notificationData.getListener());

        myNotifications.add(notification);

        if (notificationData.isBalloonNotification()) {
          applyNotification(notification);
        }
        else {
          addMessage(notification, externalSystemId, notificationData);
        }
      }
    });
  }

  public void openMessageView(@Nonnull final ProjectSystemId externalSystemId, @Nonnull final NotificationSource notificationSource) {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        prepareMessagesView(externalSystemId, notificationSource, true);
      }
    });
  }

  public void clearNotifications(@Nonnull final NotificationSource notificationSource,
                                 @Nonnull final ProjectSystemId externalSystemId) {
    clearNotifications(null, notificationSource, externalSystemId);
  }

  public void clearNotifications(@Nullable final String groupName,
                                 @Nonnull final NotificationSource notificationSource,
                                 @Nonnull final ProjectSystemId externalSystemId) {
    myMessageCounter.remove(groupName, notificationSource, externalSystemId);
    myUpdater.execute(new Runnable() {
      @Override
      public void run() {
        for (Iterator<Notification> iterator = myNotifications.iterator(); iterator.hasNext(); ) {
          Notification notification = iterator.next();
          if (groupName == null || groupName.equals(notification.getGroupId())) {
            notification.expire();
            iterator.remove();
          }
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow == null) return;

        final Pair<NotificationSource, ProjectSystemId> contentIdPair = Pair.create(notificationSource, externalSystemId);
        final MessageView messageView = ServiceManager.getService(myProject, MessageView.class);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            for (Content content : messageView.getContentManager().getContents()) {
              if (!content.isPinned() && contentIdPair.equals(content.getUserData(CONTENT_ID_KEY))) {
                if (groupName == null) {
                  messageView.getContentManager().removeContent(content, true);
                }
                else {
                  assert content.getComponent() instanceof NewEditableErrorTreeViewPanel;
                  NewEditableErrorTreeViewPanel errorTreeView = (NewEditableErrorTreeViewPanel)content.getComponent();
                  ErrorViewStructure errorViewStructure = errorTreeView.getErrorViewStructure();
                  errorViewStructure.removeGroup(groupName);
                }
              }
            }
          }
        });
      }
    });
  }

  public int getMessageCount(@Nonnull final NotificationSource notificationSource,
                             @javax.annotation.Nullable final NotificationCategory notificationCategory,
                             @Nonnull final ProjectSystemId externalSystemId) {
    return getMessageCount(null, notificationSource, notificationCategory, externalSystemId);
  }

  public int getMessageCount(@Nullable final String groupName,
                             @Nonnull final NotificationSource notificationSource,
                             @Nullable final NotificationCategory notificationCategory,
                             @Nonnull final ProjectSystemId externalSystemId) {
    return myMessageCounter.getCount(groupName, notificationSource, notificationCategory, externalSystemId);
  }

  private void addMessage(@Nonnull final Notification notification,
                          @Nonnull final ProjectSystemId externalSystemId,
                          @Nonnull final NotificationData notificationData) {
    final VirtualFile virtualFile =
            notificationData.getFilePath() != null ? ExternalSystemUtil.waitForTheFile(notificationData.getFilePath()) : null;
    final String groupName = virtualFile != null ? virtualFile.getPresentableUrl() : notificationData.getTitle();

    myMessageCounter
            .increment(groupName, notificationData.getNotificationSource(), notificationData.getNotificationCategory(), externalSystemId);

    int line = notificationData.getLine() - 1;
    int column = notificationData.getColumn() - 1;
    if (virtualFile == null) line = column = -1;
    final int guiLine = line < 0 ? -1 : line + 1;
    final int guiColumn = column < 0 ? 0 : column + 1;

    final Navigatable navigatable = notificationData.getNavigatable() != null
                                    ? notificationData.getNavigatable()
                                    : virtualFile != null ? new OpenFileDescriptor(myProject, virtualFile, line, column) : null;

    final ErrorTreeElementKind kind =
            ErrorTreeElementKind.convertMessageFromCompilerErrorType(notificationData.getNotificationCategory().getMessageCategory());
    final String[] message = notificationData.getMessage().split("\n");
    final String exportPrefix = NewErrorTreeViewPanel.createExportPrefix(guiLine);
    final String rendererPrefix = NewErrorTreeViewPanel.createRendererPrefix(guiLine, guiColumn);

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        boolean activate =
                notificationData.getNotificationCategory() == NotificationCategory.ERROR ||
                notificationData.getNotificationCategory() == NotificationCategory.WARNING;
        final NewErrorTreeViewPanel errorTreeView =
                prepareMessagesView(externalSystemId, notificationData.getNotificationSource(), activate);
        final GroupingElement groupingElement = errorTreeView.getErrorViewStructure().getGroupingElement(groupName, null, virtualFile);
        final NavigatableMessageElement navigatableMessageElement;
        if (notificationData.hasLinks()) {
          navigatableMessageElement = new EditableNotificationMessageElement(
                  notification,
                  kind,
                  groupingElement,
                  message,
                  navigatable,
                  exportPrefix,
                  rendererPrefix);
        }
        else {
          navigatableMessageElement = new NotificationMessageElement(
                  kind,
                  groupingElement,
                  message,
                  navigatable,
                  exportPrefix,
                  rendererPrefix);
        }

        errorTreeView.getErrorViewStructure().addNavigatableMessage(groupName, navigatableMessageElement);
        errorTreeView.updateTree();
      }
    });
  }

  private void applyNotification(@Nonnull final Notification notification) {
    if (!myProject.isDisposed() && myProject.isOpen()) {
      notification.notify(myProject);
    }
  }

  @Nonnull
  public NewErrorTreeViewPanel prepareMessagesView(@Nonnull final ProjectSystemId externalSystemId,
                                                   @Nonnull final NotificationSource notificationSource,
                                                   boolean activateView) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final NewErrorTreeViewPanel errorTreeView;
    final String contentDisplayName = getContentDisplayName(notificationSource, externalSystemId);
    final Pair<NotificationSource, ProjectSystemId> contentIdPair = Pair.create(notificationSource, externalSystemId);
    Content targetContent = findContent(contentIdPair, contentDisplayName);

    final MessageView messageView = ServiceManager.getService(myProject, MessageView.class);
    if (targetContent == null || !contentIdPair.equals(targetContent.getUserData(CONTENT_ID_KEY))) {
      errorTreeView = new NewEditableErrorTreeViewPanel(myProject, null, true, true, null);
      targetContent = ContentFactory.getInstance().createContent(errorTreeView, contentDisplayName, true);
      targetContent.putUserData(CONTENT_ID_KEY, contentIdPair);

      messageView.getContentManager().addContent(targetContent);
      Disposer.register(targetContent, errorTreeView);
    }
    else {
      assert targetContent.getComponent() instanceof NewEditableErrorTreeViewPanel;
      errorTreeView = (NewEditableErrorTreeViewPanel)targetContent.getComponent();
    }

    messageView.getContentManager().setSelectedContent(targetContent);
    final ToolWindow tw = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
    if (activateView && tw != null && !tw.isActive()) {
      tw.activate(null, false);
    }
    return errorTreeView;
  }

  @Nullable
  private Content findContent(@Nonnull Pair<NotificationSource, ProjectSystemId> contentIdPair, @Nonnull String contentDisplayName) {
    Content targetContent = null;
    final MessageView messageView = ServiceManager.getService(myProject, MessageView.class);
    for (Content content : messageView.getContentManager().getContents()) {
      if (contentIdPair.equals(content.getUserData(CONTENT_ID_KEY))
          && StringUtil.equals(content.getDisplayName(), contentDisplayName) && !content.isPinned()) {
        targetContent = content;
      }
    }
    return targetContent;
  }

  @Nonnull
  public static String getContentDisplayName(@Nonnull final NotificationSource notificationSource,
                                             @Nonnull final ProjectSystemId externalSystemId) {
    final String contentDisplayName;
    switch (notificationSource) {
      case PROJECT_SYNC:
        contentDisplayName =
                ExternalSystemBundle.message("notification.messages.project.sync.tab.name", externalSystemId.getReadableName());
        break;
      default:
        throw new AssertionError("unsupported notification source found: " + notificationSource);
    }
    return contentDisplayName;
  }
}