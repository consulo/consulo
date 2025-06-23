/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.application.internal.MessagePool;
import consulo.application.internal.MessagePoolListener;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.DateFormatUtil;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.internal.*;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;

public class IdeErrorsDialog extends DialogWrapper implements MessagePoolListener {
    private static final Logger LOG = Logger.getInstance(IdeErrorsDialog.class);
    private final boolean myInternalMode;
    private static final String ACTIVE_TAB_OPTION = IdeErrorsDialog.class.getName() + "activeTab";

    public static final int COMPONENTS_WIDTH = 670;

    private JPanel myContentPane;
    private JPanel myBackButtonPanel;
    private HyperlinkLabel.Croppable myInfoLabel;
    private JPanel myNextButtonPanel;
    private JPanel myTabsPanel;
    private JLabel myCountLabel;
    private HyperlinkLabel.Croppable myForeignPluginWarningLabel;
    private HyperlinkLabel.Croppable myDisableLink;
    private JPanel myCredentialsPanel;
    private HyperlinkLabel myCredentialsLabel;
    private JPanel myForeignPluginWarningPanel;
    private JPanel myAttachmentWarningPanel;
    private HyperlinkLabel myAttachmentWarningLabel;

    private int myIndex = 0;
    private final List<ArrayList<AbstractMessage>> myMergedMessages = new ArrayList<>();
    private List<AbstractMessage> myRawMessages;
    private final MessagePool myMessagePool;
    private HeaderlessTabbedPane myTabs;
    @Nullable
    private CommentsTabForm myCommentsTabForm;
    private DetailsTabForm myDetailsTabForm;
    private AttachmentsTabForm myAttachmentsTabForm;

    private ClearFatalsAction myClearAction = new ClearFatalsAction();
    private BlameAction myBlameAction = new BlameAction();
    @Nullable
    private AnalyzeAction myAnalyzeAction;
    private boolean myMute;
    private final Project myProject;

    public IdeErrorsDialog(MessagePool messagePool, @Nullable Project project, @Nullable LogMessage defaultMessage) {
        super(project, true);
        myMessagePool = messagePool;
        myProject = project;
        myInternalMode = ApplicationProperties.isInSandbox();
        setTitle(ExternalServiceLocalize.errorListTitle());
        init();
        rebuildHeaders();
        if (defaultMessage == null || !moveSelectionToMessage(defaultMessage)) {
            moveSelectionToEarliestMessage();
        }
        setCancelButtonText(CommonLocalize.closeActionName().get());
        setModal(false);
        loadDevelopersAsynchronously();
    }

    private void loadDevelopersAsynchronously() {
        Task.Backgroundable task = new Task.Backgroundable(null, "Loading developers list", true) {
            private final Collection[] myDevelopers = new Collection[]{Collections.emptyList()};

            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                try {
                    myDevelopers[0] = DevelopersLoader.fetchDevelopers(indicator);
                }
                catch (IOException e) {
                    ReportMessages.GROUP.newWarn()
                        .title(LocalizeValue.localizeTODO("Communication error"))
                        .content(LocalizeValue.localizeTODO("Unable to load developers list from server."))
                        .notify(null);
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                Collection<Developer> developers = myDevelopers[0];
                myDetailsTabForm.setDevelopers(developers);
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private boolean moveSelectionToMessage(LogMessage defaultMessage) {
        int index = -1;
        for (int i = 0; i < myMergedMessages.size(); i++) {
            final AbstractMessage each = getMessageAt(i);
            if (each == defaultMessage) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            myIndex = index;
            updateControls();
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void newEntryAdded() {
        SwingUtilities.invokeLater(() -> {
            rebuildHeaders();
            updateControls();
        });
    }

    @Override
    public void poolCleared() {
        SwingUtilities.invokeLater(() -> doOKAction());
    }

    @Override
    public void entryWasRead() {
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<>(3);
        if (Platform.current().os().isMac()) {
            actions.add(getCancelAction());
            actions.add(myClearAction);
            actions.add(myBlameAction);
        }
        else {
            actions.add(myClearAction);
            actions.add(myBlameAction);
            actions.add(getCancelAction());
        }

        ExternalServiceConfiguration externalServiceConfiguration = Application.get().getInstance(ExternalServiceConfiguration.class);

        if (externalServiceConfiguration.getState(ExternalService.ERROR_REPORTING) == ThreeState.NO) {
            actions.remove(myBlameAction);
        }

        return actions.toArray(Action[]::new);
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        getCancelAction().putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    private class ForwardAction extends AnAction implements DumbAware {
        public ForwardAction() {
            super("Next", null, AllIcons.Actions.Forward);
            AnAction forward = ActionManager.getInstance().getAction("Forward");
            if (forward != null) {
                registerCustomShortcutSet(forward.getShortcutSet(), getRootPane(), getDisposable());
            }

        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goForward();
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(myIndex < myMergedMessages.size() - 1);
        }
    }

    private class BackAction extends AnAction implements DumbAware {
        public BackAction() {
            super("Previous", null, AllIcons.Actions.Back);
            AnAction back = ActionManager.getInstance().getAction("Back");
            if (back != null) {
                registerCustomShortcutSet(back.getShortcutSet(), getRootPane(), getDisposable());
            }
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goBack();
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(myIndex > 0);
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        DefaultActionGroup goBack = new DefaultActionGroup();
        BackAction back = new BackAction();
        goBack.add(back);
        ActionToolbar backToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, goBack, true);
        backToolbar.getComponent().setBorder(IdeBorderFactory.createEmptyBorder());
        backToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        myBackButtonPanel.add(backToolbar.getComponent(), BorderLayout.CENTER);

        DefaultActionGroup goForward = new DefaultActionGroup();
        ForwardAction forward = new ForwardAction();
        goForward.add(forward);
        ActionToolbar forwardToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, goForward, true);
        forwardToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        forwardToolbar.getComponent().setBorder(IdeBorderFactory.createEmptyBorder());
        myNextButtonPanel.add(forwardToolbar.getComponent(), BorderLayout.CENTER);

        myTabs = new HeaderlessTabbedPane(getDisposable());
        final LabeledTextComponent.TextListener commentsListener = newText -> {
            if (myMute) {
                return;
            }

            AbstractMessage message = getSelectedMessage();
            if (message != null) {
                message.setAdditionalInfo(newText);
            }
        };
        if (!myInternalMode) {
            myDetailsTabForm = new DetailsTabForm(null, myInternalMode);
            myCommentsTabForm = new CommentsTabForm();
            myCommentsTabForm.addCommentsListener(commentsListener);
            myTabs.addTab(ExternalServiceLocalize.errorCommentsTabTitle().get(), myCommentsTabForm.getContentPane());
            myDetailsTabForm.setCommentsAreaVisible(false);
        }
        else {
            final AnAction analyzePlatformAction = ActionManager.getInstance().getAction("AnalyzeStacktraceOnError");
            if (analyzePlatformAction != null) {
                myAnalyzeAction = new AnalyzeAction(analyzePlatformAction);
            }
            myDetailsTabForm = new DetailsTabForm(myAnalyzeAction, myInternalMode);
            myDetailsTabForm.setCommentsAreaVisible(true);
            myDetailsTabForm.addCommentsListener(commentsListener);
        }

        myTabs.addTab(ExternalServiceLocalize.errorDetailsTabTitle().get(), myDetailsTabForm.getContentPane());

        myAttachmentsTabForm = new AttachmentsTabForm();
        myAttachmentsTabForm.addInclusionListener(e -> updateAttachmentWarning(getSelectedMessage()));

        int activeTabIndex = Integer.parseInt(ApplicationPropertiesComponent.getInstance().getValue(ACTIVE_TAB_OPTION, "0"));
        if (activeTabIndex >= myTabs.getTabCount() || activeTabIndex < 0) {
            activeTabIndex = 0; // may happen if myInternalMode changed since last open
        }

        myTabs.setSelectedIndex(activeTabIndex);

        myTabs.addChangeListener(e -> {
            final JComponent c = getPreferredFocusedComponent();
            if (c != null) {
                IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(c, true);
            }
        });

        myTabsPanel.add(myTabs, BorderLayout.CENTER);

        myDisableLink.setHyperlinkText(UIUtil.removeMnemonic(ExternalServiceLocalize.errorListDisablePlugin().get()));
        myDisableLink.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                disablePlugin();
            }
        });

        myCredentialsLabel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                updateCredentialsPane(getSelectedMessage());
            }
        });

        myAttachmentWarningLabel.setIcon(AllIcons.General.BalloonWarning);
        myAttachmentWarningLabel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                myTabs.setSelectedIndex(myTabs.indexOfComponent(myAttachmentsTabForm.getContentPane()));
                myAttachmentsTabForm.selectFirstIncludedAttachment();
            }
        });

        myDetailsTabForm.addAssigneeListener(e -> {
            if (myMute) {
                return;
            }

            AbstractMessage message = getSelectedMessage();
            if (message != null) {
                message.setAssigneeId(myDetailsTabForm.getAssigneeId());
            }
        });

        backToolbar.setTargetComponent(myContentPane);
        forwardToolbar.setTargetComponent(myContentPane);
        return myContentPane;
    }

    private void moveSelectionToEarliestMessage() {
        myIndex = 0;
        for (int i = 0; i < myMergedMessages.size(); i++) {
            final AbstractMessage each = getMessageAt(i);
            if (!each.isRead()) {
                myIndex = i;
                break;
            }
        }

        updateControls();
    }

    @RequiredUIAccess
    private void disablePlugin() {
        final PluginId pluginId = findFirstPluginId(getSelectedMessage().getThrowable());
        if (pluginId == null) {
            return;
        }

        PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(pluginId);
        final SimpleReference<Boolean> hasDependants = SimpleReference.create(false);
        PluginValidator.checkDependants(plugin, consulo.container.plugin.PluginManager::findPlugin, pluginId1 -> {
            if (PluginIds.isPlatformPlugin(pluginId1)) {
                return true;
            }
            hasDependants.set(true);
            return false;
        });

        Application app = Application.get();
        DisablePluginWarningDialog d =
            new DisablePluginWarningDialog(myProject, plugin.getName(), hasDependants.get(), app.isRestartCapable());
        d.show();
        switch (d.getExitCode()) {
            case CANCEL_EXIT_CODE:
                return;
            case DisablePluginWarningDialog.DISABLE_EXIT_CODE:
                break;
            case DisablePluginWarningDialog.DISABLE_AND_RESTART_EXIT_CODE:
                app.restart();
                break;
        }
    }

    private void goBack() {
        myIndex--;
        updateControls();
    }

    private void goForward() {
        myIndex++;
        updateControls();
    }

    private void updateControls() {
        updateCountLabel();
        final AbstractMessage message = getSelectedMessage();
        updateInfoLabel(message);
        updateCredentialsPane(message);
        myCredentialsPanel.setVisible(false);
        updateAssigneePane(message);
        updateAttachmentWarning(message);
        myDisableLink.setVisible(canDisablePlugin(message));
        updateForeignPluginLabel(message != null ? message : null);
        updateTabs();

        myClearAction.update();
        if (myAnalyzeAction != null) {
            myAnalyzeAction.update();
        }
    }

    private void updateAttachmentWarning(final AbstractMessage message) {
        final List<Attachment> includedAttachments;
        if (
            message instanceof LogMessageEx logMessageEx
                && !(includedAttachments = ContainerUtil.filter(logMessageEx.getAttachments(), Attachment::isIncluded)).isEmpty()
        ) {
            myAttachmentWarningPanel.setVisible(true);
            if (includedAttachments.size() == 1) {
                myAttachmentWarningLabel.setHtmlText(
                    ExternalServiceLocalize.diagnosticErrorReportIncludeAttachmentWarning(includedAttachments.get(0).getName()).get()
                );
            }
            else {
                myAttachmentWarningLabel.setHtmlText(
                    ExternalServiceLocalize.diagnosticErrorReportIncludeAttachmentsWarning(includedAttachments.size()).get()
                );
            }
        }
        else {
            myAttachmentWarningPanel.setVisible(false);
        }
    }

    private static boolean canDisablePlugin(AbstractMessage message) {
        if (message == null) {
            return false;
        }

        PluginId pluginId = findFirstPluginId(message.getThrowable());
        return pluginId != null;
    }

    private void updateCountLabel() {
        myCountLabel.setText(
            myMergedMessages.isEmpty()
                ? ExternalServiceLocalize.errorListEmpty().get()
                : ExternalServiceLocalize.errorListMessageIndexCount(Integer.toString(myIndex + 1), myMergedMessages.size()).get()
        );
    }

    private void updateCredentialsPane(AbstractMessage message) {
        if (message != null) {
            final ErrorReportSubmitter submitter = getSubmitter(message.getThrowable());
            if (submitter instanceof ITNReporter) {
                myCredentialsPanel.setVisible(true);
                String userName = null;
                if (StringUtil.isEmpty(userName)) {
                    myCredentialsLabel.setHtmlText(ExternalServiceLocalize.diagnosticErrorReportSubmitErrorAnonymously().get());
                }
                else {
                    myCredentialsLabel.setHtmlText(ExternalServiceLocalize.diagnosticErrorReportSubmitReportAs(userName).get());
                }
                return;
            }
        }
        myCredentialsPanel.setVisible(false);
    }

    private void updateAssigneePane(AbstractMessage message) {
        ExternalServiceConfiguration externalServiceConfiguration = Application.get().getInstance(ExternalServiceConfiguration.class);
        ThreeState devListState = externalServiceConfiguration.getState(ExternalService.DEVELOPER_LIST);

        myDetailsTabForm.setAssigneeVisible(devListState != ThreeState.NO);
    }

    private void updateInfoLabel(AbstractMessage message) {
        if (message == null) {
            myInfoLabel.setText("");
            return;
        }
        final Throwable throwable = message.getThrowable();
        if (throwable instanceof MessagePool.TooManyErrorsException) {
            myInfoLabel.setText("");
            return;
        }

        StringBuilder text = new StringBuilder();
        PluginId pluginId = findFirstPluginId(throwable);
        if (pluginId == null) {
            if (throwable instanceof AbstractMethodError) {
                text.append(ExternalServiceLocalize.errorListMessageBlameUnknownPlugin());
            }
            else {
                text.append(ExternalServiceLocalize.errorListMessageBlameCore());
            }
        }
        else {
            text.append(ExternalServiceLocalize.errorListMessageBlamePlugin(PluginManager.findPlugin(pluginId).getName()));
        }
        text.append(" ").append(ExternalServiceLocalize.errorListMessageInfo(
            DateFormatUtil.formatPrettyDateTime(message.getDate()),
            myMergedMessages.get(myIndex).size()
        ));

        String url = null;
        if (message.isSubmitted()) {
            final SubmittedReportInfo info = message.getSubmissionInfo();
            url = SubmittedReportInfo.getUrl(info);
            SubmittedReportInfoUtil.appendSubmissionInformation(info, text, url);
            text.append(". ");
        }
        else if (message.isSubmitting()) {
            text.append(" Submitting...");
        }
        else if (!message.isRead()) {
            text.append(" ").append(ExternalServiceLocalize.errorListMessageUnread());
        }
        myInfoLabel.setHtmlText(XmlStringUtil.wrapInHtml(text));
        myInfoLabel.setHyperlinkTarget(url);
    }

    private void updateForeignPluginLabel(AbstractMessage message) {
        if (message != null) {
            final Throwable throwable = message.getThrowable();
            ErrorReportSubmitter submitter = getSubmitter(throwable);
            if (submitter == null) {
                PluginId pluginId = findFirstPluginId(throwable);
                PluginDescriptor plugin = pluginId == null ? null : consulo.container.plugin.PluginManager.findPlugin(pluginId);
                if (plugin == null) {
                    // unknown plugin
                    myForeignPluginWarningPanel.setVisible(false);
                    return;
                }

                myForeignPluginWarningPanel.setVisible(true);
                String vendor = plugin.getVendor();
                String contactInfo = plugin.getVendorUrl();
                if (StringUtil.isEmpty(contactInfo)) {
                    contactInfo = plugin.getVendorEmail();
                }
                if (StringUtil.isEmpty(vendor)) {
                    if (StringUtil.isEmpty(contactInfo)) {
                        myForeignPluginWarningLabel.setText(ExternalServiceLocalize.errorDialogForeignPluginWarningText().get());
                    }
                    else {
                        myForeignPluginWarningLabel.setHyperlinkText(
                            ExternalServiceLocalize.errorDialogForeignPluginWarningTextVendor().get() + " ",
                            contactInfo,
                            "."
                        );
                        myForeignPluginWarningLabel.setHyperlinkTarget(contactInfo);
                    }
                }
                else if (StringUtil.isEmpty(contactInfo)) {
                    myForeignPluginWarningLabel.setText(
                        ExternalServiceLocalize.errorDialogForeignPluginWarningTextVendor().get() + " " + vendor + "."
                    );
                }
                else {
                    myForeignPluginWarningLabel.setHyperlinkText(
                        ExternalServiceLocalize.errorDialogForeignPluginWarningTextVendor().get()
                            + " " + vendor + " (", contactInfo, ")."
                    );
                    myForeignPluginWarningLabel.setHyperlinkTarget(contactInfo);
                }
                myForeignPluginWarningPanel.setVisible(true);
                return;
            }
        }
        myForeignPluginWarningPanel.setVisible(false);
    }

    private void updateTabs() {
        myMute = true;
        try {
            if (myInternalMode) {
                boolean hasAttachment = false;
                for (ArrayList<AbstractMessage> merged : myMergedMessages) {
                    final AbstractMessage message = merged.get(0);
                    if (message instanceof LogMessageEx && !((LogMessageEx)message).getAttachments().isEmpty()) {
                        hasAttachment = true;
                        break;
                    }
                }
                myTabs.setHeaderVisible(hasAttachment);
            }

            final AbstractMessage message = getSelectedMessage();
            if (myCommentsTabForm != null) {
                if (message != null) {
                    String msg = message.getMessage();
                    int i = msg.indexOf("\n");
                    if (i != -1) {
                        // take first line
                        msg = msg.substring(0, i);
                    }
                    myCommentsTabForm.setErrorText(msg);
                }
                else {
                    myCommentsTabForm.setErrorText(null);
                }
                if (message != null) {
                    myCommentsTabForm.setCommentText(message.getAdditionalInfo());
                    myCommentsTabForm.setCommentsTextEnabled(true);
                }
                else {
                    myCommentsTabForm.setCommentText(null);
                    myCommentsTabForm.setCommentsTextEnabled(false);
                }
            }

            myDetailsTabForm.setDetailsText(message != null ? getDetailsText(message) : null);
            if (message != null) {
                myDetailsTabForm.setCommentsText(message.getAdditionalInfo());
                myDetailsTabForm.setCommentsTextEnabled(true);
            }
            else {
                myDetailsTabForm.setCommentsText(null);
                myDetailsTabForm.setCommentsTextEnabled(false);
            }

            myDetailsTabForm.setAssigneeId(message == null ? 0 : message.getAssigneeId());

            List<Attachment> attachments =
                message instanceof LogMessageEx logMessageEx ? logMessageEx.getAttachments() : Collections.<Attachment>emptyList();
            if (!attachments.isEmpty()) {
                if (myTabs.indexOfComponent(myAttachmentsTabForm.getContentPane()) == -1) {
                    myTabs.addTab(ExternalServiceLocalize.errorAttachmentsTabTitle().get(), myAttachmentsTabForm.getContentPane());
                }
                myAttachmentsTabForm.setAttachments(attachments);
            }
            else {
                int index = myTabs.indexOfComponent(myAttachmentsTabForm.getContentPane());
                if (index != -1) {
                    myTabs.removeTabAt(index);
                }
            }
        }
        finally {
            myMute = false;
        }
    }

    private static String getDetailsText(AbstractMessage message) {
        final Throwable throwable = message.getThrowable();
        return throwable instanceof MessagePool.TooManyErrorsException
            ? throwable.getMessage()
            : message.getMessage() + "\n" + message.getThrowableText();
    }

    private void rebuildHeaders() {
        myMergedMessages.clear();
        myRawMessages = myMessagePool.getFatalErrors(true, true);

        Map<String, ArrayList<AbstractMessage>> hash2Messages = mergeMessages(myRawMessages);

        for (final ArrayList<AbstractMessage> abstractMessages : hash2Messages.values()) {
            myMergedMessages.add(abstractMessages);
        }
    }

    private void markAllAsRead() {
        for (AbstractMessage each : myRawMessages) {
            each.setRead(true);
        }
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        final int selectedIndex = myTabs.getSelectedIndex();
        JComponent result;
        if (selectedIndex == 0) {
            result = myInternalMode ? myDetailsTabForm.getPreferredFocusedComponent() : myCommentsTabForm.getPreferredFocusedComponent();
        }
        else if (selectedIndex == 1) {
            result = myInternalMode ? myAttachmentsTabForm.getPreferredFocusedComponent() : myDetailsTabForm.getPreferredFocusedComponent();
        }
        else {
            result = myAttachmentsTabForm.getPreferredFocusedComponent();
        }
        return result != null ? result : super.getPreferredFocusedComponent();
    }

    private static Map<String, ArrayList<AbstractMessage>> mergeMessages(List<AbstractMessage> aErrors) {
        Map<String, ArrayList<AbstractMessage>> hash2Messages = new LinkedHashMap<>();
        for (final AbstractMessage each : aErrors) {
            final String hashCode = getThrowableHashCode(each.getThrowable());
            ArrayList<AbstractMessage> list;
            if (hash2Messages.containsKey(hashCode)) {
                list = hash2Messages.get(hashCode);
            }
            else {
                list = new ArrayList<>();
                hash2Messages.put(hashCode, list);
            }
            list.add(0, each);
        }
        return hash2Messages;
    }

    private AbstractMessage getSelectedMessage() {
        return getMessageAt(myIndex);
    }

    private AbstractMessage getMessageAt(int idx) {
        if (idx < 0 || idx >= myMergedMessages.size()) {
            return null;
        }
        return myMergedMessages.get(idx).get(0);
    }

    @Nullable
    public static PluginId findFirstPluginId(@Nonnull Throwable t) {
        return PluginExceptionUtil.findFirstPluginId(t);
    }

    private class ClearFatalsAction extends LocalizeAction {
        protected ClearFatalsAction() {
            super(ExternalServiceLocalize.errorDialogClearAction());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            myMessagePool.clearFatals();
            doOKAction();
        }

        public void update() {
            setText(myMergedMessages.size() > 1 ? ExternalServiceLocalize.errorDialogClearAllAction() : ExternalServiceLocalize.errorDialogClearAction());
            setEnabled(!myMergedMessages.isEmpty());
        }
    }

    private class BlameAction extends LocalizeAction {
        protected BlameAction() {
            super(ExternalServiceLocalize.errorReportToConsuloAction());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean closeDialog = myMergedMessages.size() == 1;
            final AbstractMessage logMessage = getSelectedMessage();
            boolean reportingStarted = reportMessage(logMessage, closeDialog);
            if (closeDialog) {
                if (reportingStarted) {
                    doOKAction();
                }
            }
            else {
                rebuildHeaders();
                updateControls();
            }
        }

        private boolean reportMessage(final AbstractMessage logMessage, final boolean dialogClosed) {
            final ErrorReportSubmitter submitter = getSubmitter(logMessage.getThrowable());

            if (submitter != null) {
                logMessage.setSubmitting(true);
                if (!dialogClosed) {
                    updateControls();
                }
                Container parentComponent;
                if (dialogClosed) {
                    IdeFrame ideFrame = findIdeFrameFromParent(getContentPane());
                    parentComponent = ideFrame.getComponent();
                }
                else {
                    parentComponent = getContentPane();
                }
                return submitter.trySubmitAsync(
                    getEvents(logMessage),
                    logMessage.getAdditionalInfo(),
                    parentComponent,
                    submittedReportInfo -> {
                        logMessage.setSubmitting(false);
                        logMessage.setSubmitted(submittedReportInfo);
                        Application.get().invokeLater(() -> {
                            if (!dialogClosed) {
                                updateOnSubmit();
                            }
                        });
                    }
                );
            }
            return false;
        }

        @Nullable
        public static IdeFrame findIdeFrameFromParent(@Nullable Component component) {
            if (component == null) {
                return null;
            }

            Component target = component;

            while (target != null) {
                if (target instanceof Window) {
                    consulo.ui.Window uiWindow = TargetAWT.from((Window) target);

                    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
                    if (ideFrame != null) {
                        return ideFrame;
                    }
                }

                target = target.getParent();
            }

            return null;
        }

        private IdeaLoggingEvent[] getEvents(final AbstractMessage logMessage) {
            if (logMessage instanceof GroupedLogMessage) {
                final List<AbstractMessage> messages = ((GroupedLogMessage)logMessage).getMessages();
                IdeaLoggingEvent[] res = new IdeaLoggingEvent[messages.size()];
                for (int i = 0; i < res.length; i++) {
                    res[i] = getEvent(messages.get(i));
                }
                return res;
            }
            return new IdeaLoggingEvent[]{getEvent(logMessage)};
        }

        private IdeaLoggingEvent getEvent(final AbstractMessage logMessage) {
            if (logMessage instanceof LogMessageEx) {
                return ((LogMessageEx)logMessage).toEvent();
            }
            return new IdeaLoggingEvent(logMessage.getMessage(), logMessage.getThrowable()) {
                @Override
                public AbstractMessage getData() {
                    return logMessage;
                }
            };
        }
    }

    protected void updateOnSubmit() {
        updateControls();
    }

    @Nullable
    public static ErrorReportSubmitter getSubmitter(final Throwable throwable) {
        if (throwable instanceof MessagePool.TooManyErrorsException || throwable instanceof AbstractMethodError) {
            return null;
        }
        return ITNReporter.ourInternalInstance;
    }

    @Override
    public void doOKAction() {
        onClose();
        super.doOKAction();
    }

    private void onClose() {
        markAllAsRead();
        ApplicationPropertiesComponent.getInstance().setValue(ACTIVE_TAB_OPTION, String.valueOf(myTabs.getSelectedIndex()));
    }

    @Override
    public void doCancelAction() {
        onClose();
        super.doCancelAction();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "IdeErrosDialog";
    }

    private static String getThrowableHashCode(Throwable exception) {
        try {
            return md5(ExceptionUtil.getThrowableText(exception), "stack-trace");
        }
        catch (NoSuchAlgorithmException e) {
            LOG.error(e);
            return "";
        }
    }

    private static String md5(String buffer, String key) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(buffer.getBytes());
        byte[] code = md5.digest(key.getBytes());
        BigInteger bi = new BigInteger(code).abs();
        return bi.abs().toString(16);
    }

    private class AnalyzeAction extends AbstractAction {
        private final AnAction myAnalyze;

        public AnalyzeAction(AnAction analyze) {
            super(analyze.getTemplatePresentation().getText());
            myAnalyze = analyze;
        }

        public void update() {
            setEnabled(getSelectedMessage() != null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(ActionEvent e) {
            DataContext dataContext = DataManager.getInstance().getDataContext((Component) e.getSource());

            AnActionEvent event = new AnActionEvent(
                null,
                dataContext,
                ActionPlaces.UNKNOWN,
                myAnalyze.getTemplatePresentation(),
                ActionManager.getInstance(),
                e.getModifiers()
            );

            final Project project = dataContext.getData(Project.KEY);
            if (project != null) {
                myAnalyze.actionPerformed(event);
                doOKAction();
            }
        }
    }
}
