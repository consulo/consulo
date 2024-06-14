package consulo.ide.impl.idea.openapi.vcs;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import org.jetbrains.annotations.NonNls;

/**
 * @author irengrig
 * Date: 5/20/11
 * Time: 12:33 PM
 */
public class ReadonlyStatusIsVisibleActivationCheck {
  @NonNls
  public static void check(final Project project, final String vcsName, NotificationGroup vcsNotificationGroup) {
    if (Platform.current().os().isUnix() && "root".equals(System.getenv("USER"))) {
      Notifications.Bus.notify(
        new Notification(
          vcsNotificationGroup,
          vcsName + ": can not see read-only status",
          "You are logged as <b>root</b>, that's why:<br><br>- " +
            Application.get().getName().get() +
            " can not see read-only status of files.<br>" +
            "- All files are treated as writeable.<br>- Automatic file checkout on modification is impossible.",
          NotificationType.WARNING
        ),
        project
      );
    }
  }
}
