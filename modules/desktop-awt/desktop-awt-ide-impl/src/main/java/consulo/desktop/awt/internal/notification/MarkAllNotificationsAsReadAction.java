package consulo.desktop.awt.internal.notification;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public class MarkAllNotificationsAsReadAction extends DumbAwareAction {
    public MarkAllNotificationsAsReadAction() {
        super(
            ProjectUILocalize.actionNotificationMarkAsReadText(),
            ProjectUILocalize.actionNotificationMarkAsReadDescription(),
            PlatformIconGroup.actionsSelectall()
        );
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(!EventLog.getLogModel(e.getData(Project.KEY)).getNotifications().isEmpty());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        EventLog.markAllAsRead(e.getData(Project.KEY));
    }
}
