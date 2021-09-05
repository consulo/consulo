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
import consulo.externalService.AuthorizationFailedException;
import consulo.externalService.UpdateAvailableException;
import com.intellij.ide.DataManager;
import com.intellij.idea.ActionsBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.CheckForUpdateAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.xml.util.XmlStringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.external.api.ErrorReportBean;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceException;
import consulo.logging.Logger;
import consulo.platform.impl.action.LastActionTracker;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Set;

/**
 * @author max
 */
public class ITNReporter extends ErrorReportSubmitter {
  private static final Logger LOG = Logger.getInstance(ITNReporter.class);

  public static final ITNReporter ourInternalInstance = new ITNReporter();

  private static String ourPreviousErrorReporterId;

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
    ErrorReportBean errorBean = new ErrorReportBean(UpdateSettings.getInstance().getChannel(), event.getThrowable(), LastActionTracker.ourLastActionId);

    return doSubmit(event, parentComponent, callback, errorBean, additionalInfo);
  }

  private static boolean doSubmit(final IdeaLoggingEvent event,
                                  final Component parentComponent,
                                  final Consumer<SubmittedReportInfo> callback,
                                  final ErrorReportBean errorBean,
                                  final String description) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);

    errorBean.setInstallationID(PermanentInstallationID.get());
    errorBean.setDescription(description);
    errorBean.setMessage(event.getMessage());

    if (ourPreviousErrorReporterId != null) {
      errorBean.setPreviousException(ourPreviousErrorReporterId);
    }

    Throwable t = event.getThrowable();
    if (t != null) {
      Set<PluginId> pluginIds = IdeErrorsDialog.findAllPluginIds(t);
      for (PluginId pluginId : pluginIds) {
        final PluginDescriptor pluginDescriptor = PluginManager.findPlugin(pluginId);
        if (pluginDescriptor != null) {
          String version = pluginDescriptor.getVersion();
          if(StringUtil.isEmpty(version)) {
            if(PluginIds.isPlatformPlugin(pluginId)) {
              version = ApplicationInfo.getInstance().getBuild().asString();
            }
          }

          if (StringUtil.isEmpty(version)) {
            LOG.error("There not version for plugin: " + pluginId + ", name: " + pluginDescriptor.getName());
            continue;
          }
          errorBean.addAffectedPlugin(pluginId, version);
        }
      }
    }

    Object data = event.getData();

    long assignUserId = 0;

    if (data instanceof AbstractMessage abstractMessage) {
      assignUserId = abstractMessage.getAssigneeId();
    }

    if (data instanceof LogMessageEx) {
      errorBean.setAttachments(((LogMessageEx)data).getAttachments());
    }

    // TODO send assignUserId
    
    ErrorReportSender.sendReport(project, errorBean, assignUserId, id -> {
      ourPreviousErrorReporterId = id;
      String shortId = id.substring(0, 8);
      final SubmittedReportInfo reportInfo = new SubmittedReportInfo(WebServiceApi.ERROR_REPORT.buildUrl("#" + id), shortId, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
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
          @RequiredUIAccess
          @Override
          public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
            CheckForUpdateAction.actionPerformed(e.getData(CommonDataKeys.PROJECT), UpdateSettings.getInstance(), UIAccess.current());
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

  private static void showMessageDialog(Component parentComponent, Project project, String message, String title, Image icon) {
    if (parentComponent.isShowing()) {
      Messages.showMessageDialog(parentComponent, message, title, icon);
    }
    else {
      Messages.showMessageDialog(project, message, title, icon);
    }
  }

  private static int showYesNoDialog(Component parentComponent, Project project, String message, String title, Image icon) {
    if (parentComponent.isShowing()) {
      return Messages.showYesNoDialog(parentComponent, message, title, icon);
    }
    else {
      return Messages.showYesNoDialog(project, message, title, icon);
    }
  }
}
