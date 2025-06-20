package consulo.desktop.awt.internal.notification;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public class MarkAllNotificationsAsReadAction extends DumbAwareAction {
    public MarkAllNotificationsAsReadAction() {
        super(
            LocalizeValue.localizeTODO("Mark all notifications as read"),
            LocalizeValue.localizeTODO("Mark all unread notifications as read"),
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
