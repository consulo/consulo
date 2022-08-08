package consulo.ide.impl.idea.openapi.vcs;

import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.application.util.SystemInfo;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;

/**
 * @author irengrig
 * Date: 5/20/11
 * Time: 12:33 PM
 */
public class ReadonlyStatusIsVisibleActivationCheck {
  public static void check(final Project project, final String vcsName, NotificationGroup vcsNotificationGroup) {
    if (SystemInfo.isUnix && "root".equals(System.getenv("USER"))) {
      Notifications.Bus.notify(new Notification(vcsNotificationGroup, vcsName + ": can not see read-only status", "You are logged as <b>root</b>, that's why:<br><br>- " +
                                                                                                                  ApplicationNamesInfo.getInstance().getFullProductName() +
                                                                                                                  " can not see read-only status of files.<br>" +
                                                                                                                  "- All files are treated as writeable.<br>- Automatic file checkout on modification is impossible.",
                                                NotificationType.WARNING), project);
    }
  }
}
