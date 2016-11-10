/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.diagnostic;

import com.intellij.errorreport.ErrorReportSender;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.errorreport.error.AuthorizationFailedException;
import com.intellij.errorreport.error.UpdateAvailableException;
import com.intellij.errorreport.error.WebServiceException;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.ActionsBundle;
import com.intellij.idea.IdeaLogger;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.CheckForUpdateAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.xml.util.XmlStringUtil;
import consulo.ide.updateSettings.UpdateSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;

/**
 * @author max
 */
public class ITNReporter extends ErrorReportSubmitter {
  public static final ITNReporter ourInternalInstance = new ITNReporter();

  private static String previousExceptionThreadId;

  @Override
  public boolean trySubmitAsync(IdeaLoggingEvent[] events, String additionalInfo, Component parentComponent, Consumer<SubmittedReportInfo> consumer) {
    return sendError(events[0], additionalInfo, parentComponent, consumer);
  }

  /**
   * @noinspection ThrowablePrintStackTrace
   */
  private static boolean sendError(IdeaLoggingEvent event,
                                   String additionalInfo,
                                   final Component parentComponent,
                                   final Consumer<SubmittedReportInfo> callback) {
    ErrorBean errorBean = new ErrorBean(event.getThrowable(), IdeaLogger.ourLastActionId);

    return doSubmit(event, parentComponent, callback, errorBean, additionalInfo);
  }

  private static boolean doSubmit(final IdeaLoggingEvent event,
                                  final Component parentComponent,
                                  final Consumer<SubmittedReportInfo> callback,
                                  final ErrorBean errorBean,
                                  final String description) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    errorBean.setDescription(description);
    errorBean.setMessage(event.getMessage());

    if (previousExceptionThreadId != null) {
      errorBean.setPreviousException(previousExceptionThreadId);
    }

    Throwable t = event.getThrowable();
    if (t != null) {
      Set<PluginId> pluginIds = IdeErrorsDialog.findPluginIds(t);
      Map<String, String> affectedPluginIds = errorBean.getAffectedPluginIds();
      for (PluginId pluginId : pluginIds) {
        final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
        if (pluginDescriptor != null) {
          affectedPluginIds.put(pluginId.getIdString(), StringUtil.notNullize(pluginDescriptor.getVersion(), "?"));
        }
      }
    }

    Object data = event.getData();

    if (data instanceof AbstractMessage) {
      errorBean.setAssigneeId(((AbstractMessage)data).getAssigneeId());
    }

    if (data instanceof LogMessageEx) {
      errorBean.setAttachments(((LogMessageEx)data).getAttachments());
    }

    ErrorReportSender.sendReport(project, null, errorBean, id -> {
      previousExceptionThreadId = id;
      final SubmittedReportInfo reportInfo = new SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
      callback.consume(reportInfo);

      ApplicationManager.getApplication().invokeLater(() -> {
        StringBuilder text = new StringBuilder();
        final String url = IdeErrorsDialog.getUrl(reportInfo);

        IdeErrorsDialog.appendSubmissionInformation(reportInfo, text, url);

        text.append(".");

        if (reportInfo.getStatus() != SubmittedReportInfo.SubmissionStatus.FAILED) {
          text.append("<br/>").append(DiagnosticBundle.message("error.report.gratitude"));
        }

        NotificationType type = reportInfo.getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED ? NotificationType.ERROR : NotificationType.INFORMATION;
        NotificationListener listener = url != null ? new NotificationListener.UrlOpeningListener(true) : null;
        ReportMessages.GROUP.createNotification(ReportMessages.ERROR_REPORT, XmlStringUtil.wrapInHtml(text), type, listener).setImportant(false)
                .notify(project);
      });
    }, e -> ApplicationManager.getApplication().invokeLater(() -> {
      String msg;
      if (e instanceof AuthorizationFailedException) {
        msg = DiagnosticBundle.message("error.report.authentication.failed");
      }
      else if (e instanceof WebServiceException) {
        msg = DiagnosticBundle.message("error.report.posting.failed", e.getMessage());
      }
      else {
        msg = DiagnosticBundle.message("error.report.sending.failure");
      }
      if (e instanceof UpdateAvailableException) {
        callback.consume(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));

        Notification notification =
                ReportMessages.GROUP.createNotification(DiagnosticBundle.message("error.report.update.required.message"), NotificationType.INFORMATION);
        notification.setTitle(ReportMessages.ERROR_REPORT);
        notification.setImportant(false);
        notification.addAction(new NotificationAction(ActionsBundle.actionText("CheckForUpdate")) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
            CheckForUpdateAction.actionPerformed(e.getData(CommonDataKeys.PROJECT), UpdateSettings.getInstance());
          }
        });
        notification.notify(project);
      }
      else if (showYesNoDialog(parentComponent, project, msg, ReportMessages.ERROR_REPORT, Messages.getErrorIcon()) != 0) {
        callback.consume(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));
      }
      else {
        if (e instanceof AuthorizationFailedException) {
          // TODO [VISTALL]
        }
        ApplicationManager.getApplication().invokeLater(() -> doSubmit(event, parentComponent, callback, errorBean, description));
      }
    }));
    return true;
  }

  private static void showMessageDialog(Component parentComponent, Project project, String message, String title, Icon icon) {
    if (parentComponent.isShowing()) {
      Messages.showMessageDialog(parentComponent, message, title, icon);
    }
    else {
      Messages.showMessageDialog(project, message, title, icon);
    }
  }

  private static int showYesNoDialog(Component parentComponent, Project project, String message, String title, Icon icon) {
    if (parentComponent.isShowing()) {
      return Messages.showYesNoDialog(parentComponent, message, title, icon);
    }
    else {
      return Messages.showYesNoDialog(project, message, title, icon);
    }
  }
}
