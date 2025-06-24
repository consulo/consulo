package consulo.ide.impl.idea.openapi.vcs;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;

/**
 * @author irengrig
 * @since 2011-05-20
 */
public class ReadonlyStatusIsVisibleActivationCheck {
  public static void check(Project project, String vcsName, NotificationGroup vcsNotificationGroup) {
    if (Platform.current().os().isUnix() && "root".equals(System.getenv("USER"))) {
      vcsNotificationGroup.newWarn()
          .title(LocalizeValue.localizeTODO(vcsName + ": can not see read-only status"))
          .content(LocalizeValue.localizeTODO(
            "You are logged as <b>root</b>, that's why:<br><br>- " +
              Application.get().getName().get() +
              " can not see read-only status of files.<br>" +
              "- All files are treated as writeable.<br>- Automatic file checkout on modification is impossible."
          ))
          .notify(project);
    }
  }
}
