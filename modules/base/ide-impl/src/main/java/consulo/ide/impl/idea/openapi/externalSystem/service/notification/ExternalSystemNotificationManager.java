package consulo.ide.impl.idea.openapi.externalSystem.service.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.externalSystem.service.notification.ExternalSystemNotificationExtension;
import consulo.externalSystem.service.notification.NotificationCategory;
import consulo.externalSystem.service.notification.NotificationData;
import consulo.externalSystem.service.notification.NotificationSource;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import consulo.util.rmi.RemoteUtil;
import consulo.ide.impl.idea.ide.errorTreeView.*;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.ServiceManager;
import consulo.externalSystem.ExternalSystemConfigurableAware;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.rt.model.LocationAwareExternalSystemException;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.ide.impl.idea.openapi.externalSystem.util.ExternalSystemUtil;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.navigation.Navigatable;
import consulo.fileEditor.EditorNotifications;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.project.ui.view.MessageView;
import consulo.util.lang.ObjectUtil;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.UIUtil;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.inject.Singleton;

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
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
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

    for (ExternalSystemNotificationExtension extension : myProject.getApplication().getExtensionList(ExternalSystemNotificationExtension.class)) {
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
            app.invokeAndWait(action, IdeaModalityState.defaultModalityState());
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
                             @jakarta.annotation.Nullable final NotificationCategory notificationCategory,
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
                                    : virtualFile != null ? new OpenFileDescriptorImpl(myProject, virtualFile, line, column) : null;

    final ErrorTreeElementKind kind =
            ErrorTreeElementKind.convertMessageFromCompilerErrorType(notificationData.getNotificationCategory().getMessageCategory());
    final String[] message = notificationData.getMessage().split("\n");
    final String exportPrefix = NewErrorTreeViewPanelImpl.createExportPrefix(guiLine);
    final String rendererPrefix = NewErrorTreeViewPanelImpl.createRendererPrefix(guiLine, guiColumn);

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        boolean activate =
                notificationData.getNotificationCategory() == NotificationCategory.ERROR ||
                notificationData.getNotificationCategory() == NotificationCategory.WARNING;
        final NewErrorTreeViewPanelImpl errorTreeView =
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
  public NewErrorTreeViewPanelImpl prepareMessagesView(@Nonnull final ProjectSystemId externalSystemId,
                                                       @Nonnull final NotificationSource notificationSource,
                                                       boolean activateView) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final NewErrorTreeViewPanelImpl errorTreeView;
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