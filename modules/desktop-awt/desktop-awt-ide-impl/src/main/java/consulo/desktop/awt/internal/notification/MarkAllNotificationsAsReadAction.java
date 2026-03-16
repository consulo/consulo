package consulo.desktop.awt.internal.notification;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

public class MarkAllNotificationsAsReadAction extends DumbAwareAction {
    public MarkAllNotificationsAsReadAction() {
        super(
            ProjectUILocalize.actionNotificationMarkAsReadText(),
            ProjectUILocalize.actionNotificationMarkAsReadDescription(),
            PlatformIconGroup.actionsSelectall()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(!EventLog.getLogModel(e.getData(Project.KEY)).getNotifications().isEmpty());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        EventLog.markAllAsRead(e.getData(Project.KEY));
    }
}
