package consulo.versionControlSystem.internal;

import consulo.application.CommonBundle;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.versionControlSystem.VcsNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class FilesProcessorWithNotificationImpl extends FilesProcessorImpl {
  private final Object NOTIFICATION_LOCK = new Object();

  private Notification notification = null;

  private String forAllProjectsActionText = null;

  private String viewFilesDialogTitle = null;

  private String viewFilesDialogOkActionName = CommonBundle.getAddButtonText();

  private String viewFilesDialogCancelActionName = CommonBundle.getCancelButtonText();

  public FilesProcessorWithNotificationImpl(@Nonnull Project project, @Nonnull Disposable parentDisposable) {
    super(project, parentDisposable);
  }

  @Nonnull
  public abstract String getNotificationDisplayId();

  @Nonnull
  public abstract String getAskedBeforeProperty();

  @Nonnull
  public abstract String getDoForCurrentProjectProperty();

  @Nonnull
  public abstract String getShowActionText();

  @Nonnull
  public abstract String getForCurrentProjectActionText();

  @Nullable
  public String getForAllProjectsActionText() {
    return forAllProjectsActionText;
  }

  @Nonnull
  public abstract String getMuteActionText();

  @Nonnull
  public abstract String notificationTitle();

  @Nonnull
  public abstract String notificationMessage();

  @Nullable
  protected String getViewFilesDialogTitle() {
    return viewFilesDialogTitle;
  }

  @Nonnull
  protected String getViewFilesDialogOkActionName() {
    return viewFilesDialogOkActionName;
  }

  @Nonnull
  protected String getViewFilesDialogCancelActionName() {
    return viewFilesDialogCancelActionName;
  }

  protected void handleProcessingForCurrentProject() {
    synchronized(NOTIFICATION_LOCK) {
      if (notAskedBefore() && notificationNotPresent()) {
        List<NotificationAction> notificationActions = apply(mutableListOf(showAction(), addForCurrentProjectAction()), () -> {
          if (getForAllProjectsActionText() != null) {
                add(forAllProjectsAction());
              }
              ;
          add(muteAction());
        });
        notification = VcsNotifier.getInstance(getProject()).notifyMinorInfo(getNotificationDisplayId(), true, notificationTitle(), notificationMessage(), toTypedArray(notificationActions));
      }
    }
  }

  private NotificationAction showAction() {
    return NotificationAction.createSimple(getShowActionText(), () -> {
      List<VirtualFile> allFiles = selectValidFiles();
      if (isNotEmpty(allFiles)) {
            SelectFilesDialog dialog = SelectFilesDialog.init(getProject(), allFiles, null, null, true, true, getViewFilesDialogOkActionName(), getViewFilesDialogCancelActionName());
            dialog.setTitle(getViewFilesDialogTitle());
            dialog.setSelectedFiles(allFiles);
            if (dialog.showAndGet()) {
              Collection<VirtualFile> userSelectedFiles = dialog.getSelectedFiles();
              doActionOnChosenFiles(userSelectedFiles);
              removeFiles(userSelectedFiles);
              if (isFilesEmpty()) {
                expireNotification();
              }
            }
          }
    });
  }

  private NotificationAction addForCurrentProjectAction() {
    return NotificationAction.create(getForCurrentProjectActionText(), (e, n) -> {
      doActionOnChosenFiles(acquireValidFiles());
      setForCurrentProject(true);
      ProjectPropertiesComponent.getInstance(getProject()).setValue(getAskedBeforeProperty(), true);
      expireNotification();
    });
  }

  private NotificationAction forAllProjectsAction() {
    return NotificationAction.create(getForAllProjectsActionText(), (e, n) -> {
      doActionOnChosenFiles(acquireValidFiles());
      setForCurrentProject(true);
      ProjectPropertiesComponent.getInstance(getProject()).setValue(getAskedBeforeProperty(), true);
      rememberForAllProjects();
      expireNotification();
    });
  }

  private NotificationAction muteAction() {
    return NotificationAction.create(getMuteActionText(), (e, notification) -> {
      setForCurrentProject(false);
      ProjectPropertiesComponent.getInstance(getProject()).setValue(getAskedBeforeProperty(), true);
      notification.expire();
    });
  }

  private boolean notificationNotPresent() {
    synchronized(NOTIFICATION_LOCK) {
      return notification == null || notification.isExpired();
    }
  }

  private void expireNotification() {
    synchronized(NOTIFICATION_LOCK) {
      notification.expire();
    }
  }

  protected void rememberForAllProjects() {
    throw new UnsupportedOperationException();
  }

  protected void setForCurrentProject(boolean isEnabled) {
    ProjectPropertiesComponent.getInstance(getProject()).setValue(getDoForCurrentProjectProperty(), isEnabled);
  }

  protected boolean needDoForCurrentProject() {
    return ProjectPropertiesComponent.getInstance(getProject()).getBoolean(getDoForCurrentProjectProperty(), false);
  }

  private boolean notAskedBefore() {
    return !wasAskedBefore();
  }

  private boolean wasAskedBefore() {
    return ProjectPropertiesComponent.getInstance(getProject()).getBoolean(getAskedBeforeProperty(), false);
  }
}
