package consulo.ide.impl.idea.notification.impl.actions;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.notification.EventLog;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

public class MarkAllNotificationsAsReadAction extends DumbAwareAction {
  public MarkAllNotificationsAsReadAction() {
    super("Mark all notifications as read", "Mark all unread notifications as read", AllIcons.Actions.Selectall);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(!EventLog.getLogModel(e.getData(CommonDataKeys.PROJECT)).getNotifications().isEmpty());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    EventLog.markAllAsRead(e.getData(CommonDataKeys.PROJECT));
  }
}
