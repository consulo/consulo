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
package consulo.externalService.impl.internal.errorReport;

import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.internal.LastActionTracker;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.impl.internal.PermanentInstallationID;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.WebServiceException;
import consulo.externalService.impl.internal.repository.AuthorizationFailedException;
import consulo.externalService.impl.internal.repository.UpdateAvailableException;
import consulo.externalService.impl.internal.repository.api.ErrorReportBean;
import consulo.externalService.impl.internal.update.CheckForUpdateAction;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.logging.internal.AbstractMessage;
import consulo.logging.internal.IdeaLoggingEvent;
import consulo.logging.internal.LogMessageEx;
import consulo.logging.internal.SubmittedReportInfo;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;

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
        final Project project = dataContext.getData(Project.KEY);

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
                    if (StringUtil.isEmpty(version)) {
                        if (PluginIds.isPlatformPlugin(pluginId)) {
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
            errorBean.setAttachments(((LogMessageEx) data).getAttachments());
        }

        Application application = Application.get();
        ErrorReportSender.sendReport(project, errorBean, assignUserId, id -> {
            ourPreviousErrorReporterId = id;
            String shortId = id.substring(0, 8);
            final SubmittedReportInfo reportInfo = new SubmittedReportInfo(WebServiceApi.ERROR_REPORT.buildUrl(id), shortId, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
            callback.accept(reportInfo);

            application.invokeLater(() -> {
                StringBuilder text = new StringBuilder();
                final String url = SubmittedReportInfo.getUrl(reportInfo);

                SubmittedReportInfoUtil.appendSubmissionInformation(reportInfo, text, url);

                text.append(".");

                if (reportInfo.getStatus() != SubmittedReportInfo.SubmissionStatus.FAILED) {
                    text.append("<br/>").append(ExternalServiceLocalize.errorReportGratitude().get());
                }

                ReportMessages.GROUP.buildNotification(
                        reportInfo.getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED
                            ? NotificationType.ERROR
                            : NotificationType.INFORMATION
                    )
                    .title(ExternalServiceLocalize.errorReportTitle())
                    .content(LocalizeValue.localizeTODO(XmlStringUtil.wrapInHtml(text)))
                    .notImportant()
                    .optionalListener(url != null ? new NotificationListener.UrlOpeningListener(true) : null)
                    .notify(project);
            });
        }, e -> application.invokeLater(() -> {
            String msg;
            if (e instanceof AuthorizationFailedException) {
                msg = ExternalServiceLocalize.errorReportAuthenticationFailed().get();
            }
            else if (e instanceof WebServiceException) {
                msg = ExternalServiceLocalize.errorReportPostingFailed(e.getMessage()).get();
            }
            else {
                msg = ExternalServiceLocalize.errorReportSendingFailure().get();
            }

            if (e instanceof UpdateAvailableException) {
                callback.accept(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));

                ReportMessages.GROUP.buildInfo()
                    .title(ExternalServiceLocalize.errorReportTitle())
                    .content(ExternalServiceLocalize.errorReportUpdateRequiredMessage())
                    .notImportant()
                    .addAction(
                        ActionLocalize.actionCheckforupdateText(),
                        evt ->
                            CheckForUpdateAction.actionPerformed(evt.getData(Project.KEY), UpdateSettings.getInstance(), UIAccess.current())
                    )
                    .notify(project);
            }
            else if (showYesNoDialog(parentComponent, project, msg, ExternalServiceLocalize.errorReportTitle().get(), Messages.getErrorIcon()) != 0) {
                callback.accept(new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED));
            }
            else {
                if (e instanceof AuthorizationFailedException) {
                    // TODO [VISTALL]
                }
                application.invokeLater(() -> doSubmit(event, parentComponent, callback, errorBean, description));
            }
        }));
        return true;
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
