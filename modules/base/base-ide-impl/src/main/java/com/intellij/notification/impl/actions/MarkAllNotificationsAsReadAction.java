package com.intellij.notification.impl.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.EventLog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import javax.annotation.Nonnull;
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
