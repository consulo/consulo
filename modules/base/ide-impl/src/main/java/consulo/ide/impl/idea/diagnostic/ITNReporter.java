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
package consulo.ide.impl.idea.diagnostic;

import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.logging.IdeaLoggingEvent;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.impl.internal.PermanentInstallationID;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.actionSystem.impl.LastActionTracker;
import consulo.ide.impl.external.api.ErrorReportBean;
import consulo.ide.impl.externalService.AuthorizationFailedException;
import consulo.ide.impl.externalService.UpdateAvailableException;
import consulo.ide.impl.externalService.impl.WebServiceApi;
import consulo.ide.impl.externalService.impl.WebServiceException;
import consulo.ide.impl.idea.errorreport.ErrorReportSender;
import consulo.ide.impl.idea.notification.NotificationAction;
import consulo.ide.impl.idea.openapi.diagnostic.ErrorReportSubmitter;
import consulo.ide.impl.idea.openapi.diagnostic.SubmittedReportInfo;
import consulo.ide.impl.idea.openapi.updateSettings.impl.CheckForUpdateAction;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.impl.updateSettings.UpdateSettingsImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;

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
      Set<PluginId> pluginIds = PluginExceptionUtil.findAllPluginIds(t);
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
      callback.accept(reportInfo);

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
        callback.accept(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));

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
        callback.accept(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));
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
