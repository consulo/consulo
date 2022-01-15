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
package com.intellij.diagnostic;

import com.intellij.CommonBundle;
import com.intellij.diagnostic.errordialog.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.ExtensionException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.HeaderlessTabbedPane;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import consulo.application.ApplicationProperties;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.desktop.wm.impl.DesktopIdeFrameUtil;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.ide.base.BaseDataManager;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;

public class IdeErrorsDialog extends DialogWrapper implements MessagePoolListener, TypeSafeDataProvider {
  private static final Logger LOG = Logger.getInstance(IdeErrorsDialog.class);
  private final boolean myInternalMode;
  private static final String ACTIVE_TAB_OPTION = IdeErrorsDialog.class.getName() + "activeTab";
  public static Key<String> CURRENT_TRACE_KEY = Key.create("current_stack_trace_key");

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
    setTitle(DiagnosticBundle.message("error.list.title"));
    init();
    rebuildHeaders();
    if (defaultMessage == null || !moveSelectionToMessage(defaultMessage)) {
      moveSelectionToEarliestMessage();
    }
    setCancelButtonText(CommonBundle.message("close.action.name"));
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
          Notifications.Bus.register("Error reporter", NotificationDisplayType.BALLOON);
          Notifications.Bus.notify(new Notification("Error reporter", "Communication error",
                                                    "Unable to load developers list from server.", NotificationType.WARNING));
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
    if (SystemInfo.isMac) {
      actions.add(getCancelAction());
      actions.add(myClearAction);
      actions.add(myBlameAction);
    }
    else {
      actions.add(myClearAction);
      actions.add(myBlameAction);
      actions.add(getCancelAction());
    }

    ExternalServiceConfiguration externalServiceConfiguration = ServiceManager.getService(ExternalServiceConfiguration.class);

    if(externalServiceConfiguration.getState(ExternalService.ERROR_REPORTING) == ThreeState.NO) {
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
    public void actionPerformed(AnActionEvent e) {
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
    public void actionPerformed(AnActionEvent e) {
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
    final LabeledTextComponent.TextListener commentsListener = new LabeledTextComponent.TextListener() {
      @Override
      public void textChanged(String newText) {
        if (myMute) {
          return;
        }

        AbstractMessage message = getSelectedMessage();
        if (message != null) {
          message.setAdditionalInfo(newText);
        }
      }
    };
    if (!myInternalMode) {
      myDetailsTabForm = new DetailsTabForm(null, myInternalMode);
      myCommentsTabForm = new CommentsTabForm();
      myCommentsTabForm.addCommentsListener(commentsListener);
      myTabs.addTab(DiagnosticBundle.message("error.comments.tab.title"), myCommentsTabForm.getContentPane());
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

    myTabs.addTab(DiagnosticBundle.message("error.details.tab.title"), myDetailsTabForm.getContentPane());

    myAttachmentsTabForm = new AttachmentsTabForm();
    myAttachmentsTabForm.addInclusionListener(new ChangeListener() {
      @Override
      public void stateChanged(final ChangeEvent e) {
        updateAttachmentWarning(getSelectedMessage());
      }
    });

    int activeTabIndex = Integer.parseInt(PropertiesComponent.getInstance().getValue(ACTIVE_TAB_OPTION, "0"));
    if (activeTabIndex >= myTabs.getTabCount() || activeTabIndex < 0) {
      activeTabIndex = 0; // may happen if myInternalMode changed since last open
    }

    myTabs.setSelectedIndex(activeTabIndex);

    myTabs.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        final JComponent c = getPreferredFocusedComponent();
        if (c != null) {
          IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(c, true);
        }
      }
    });

    myTabsPanel.add(myTabs, BorderLayout.CENTER);

    myDisableLink.setHyperlinkText(UIUtil.removeMnemonic(DiagnosticBundle.message("error.list.disable.plugin")));
    myDisableLink.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          disablePlugin();
        }
      }
    });

    myCredentialsLabel.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          updateCredentialsPane(getSelectedMessage());
        }
      }
    });

    myAttachmentWarningLabel.setIcon(AllIcons.General.BalloonWarning);
    myAttachmentWarningLabel.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(final HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          myTabs.setSelectedIndex(myTabs.indexOfComponent(myAttachmentsTabForm.getContentPane()));
          myAttachmentsTabForm.selectFirstIncludedAttachment();
        }
      }
    });

    myDetailsTabForm.addAssigneeListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myMute) return;

        AbstractMessage message = getSelectedMessage();
        if (message != null) {
          message.setAssigneeId(myDetailsTabForm.getAssigneeId());
        }
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

  private void disablePlugin() {
    final PluginId pluginId = findFirstPluginId(getSelectedMessage().getThrowable());
    if (pluginId == null) {
      return;
    }

    PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(pluginId);
    final SimpleReference<Boolean> hasDependants = SimpleReference.create(false);
    consulo.container.plugin.PluginManager.checkDependants(plugin, PluginManager::getPlugin, pluginId1 -> {
      if (PluginManager.isSystemPlugin(pluginId1)) {
        return true;
      }
      hasDependants.set(true);
      return false;
    });

    Application app = ApplicationManager.getApplication();
    DisablePluginWarningDialog d = new DisablePluginWarningDialog(myProject, plugin.getName(), hasDependants.get(), app.isRestartCapable());
    d.show();
    switch (d.getExitCode()) {
      case CANCEL_EXIT_CODE:
        return;
      case DisablePluginWarningDialog.DISABLE_EXIT_CODE:
        consulo.container.plugin.PluginManager.disablePlugin(pluginId.getIdString());
        break;
      case DisablePluginWarningDialog.DISABLE_AND_RESTART_EXIT_CODE:
        consulo.container.plugin.PluginManager.disablePlugin(pluginId.getIdString());
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
    if (message instanceof LogMessageEx && !(includedAttachments = ContainerUtil.filter(((LogMessageEx)message).getAttachments(), new Condition<Attachment>() {
      @Override
      public boolean value(final Attachment attachment) {
        return attachment.isIncluded();
      }
    })).isEmpty()) {
      myAttachmentWarningPanel.setVisible(true);
      if (includedAttachments.size() == 1) {
        myAttachmentWarningLabel.setHtmlText(DiagnosticBundle.message("diagnostic.error.report.include.attachment.warning", includedAttachments.get(0).getName()));
      }
      else {
        myAttachmentWarningLabel.setHtmlText(DiagnosticBundle.message("diagnostic.error.report.include.attachments.warning", includedAttachments.size()));
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
    if (pluginId == null) {
      return false;
    }
    return true;
  }

  private void updateCountLabel() {
    if (myMergedMessages.isEmpty()) {
      myCountLabel.setText(DiagnosticBundle.message("error.list.empty"));
    }
    else {
      myCountLabel.setText(DiagnosticBundle.message("error.list.message.index.count", Integer.toString(myIndex + 1), myMergedMessages.size()));
    }
  }


  private void updateCredentialsPane(AbstractMessage message) {
    if (message != null) {
      final ErrorReportSubmitter submitter = getSubmitter(message.getThrowable());
      if (submitter instanceof ITNReporter) {
        myCredentialsPanel.setVisible(true);
        String userName = null;
        if (StringUtil.isEmpty(userName)) {
          myCredentialsLabel.setHtmlText(DiagnosticBundle.message("diagnostic.error.report.submit.error.anonymously"));
        }
        else {
          myCredentialsLabel.setHtmlText(DiagnosticBundle.message("diagnostic.error.report.submit.report.as", userName));
        }
        return;
      }
    }
    myCredentialsPanel.setVisible(false);
  }

  private void updateAssigneePane(AbstractMessage message) {
    ExternalServiceConfiguration externalServiceConfiguration = ServiceManager.getService(ExternalServiceConfiguration.class);
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
        text.append(DiagnosticBundle.message("error.list.message.blame.unknown.plugin"));
      }
      else {
        text.append(DiagnosticBundle.message("error.list.message.blame.core"));
      }
    }
    else {
      text.append(DiagnosticBundle.message("error.list.message.blame.plugin", PluginManager.getPlugin(pluginId).getName()));
    }
    text.append(" ").append(DiagnosticBundle.message("error.list.message.info", DateFormatUtil.formatPrettyDateTime(message.getDate()), myMergedMessages.get(myIndex).size()));

    String url = null;
    if (message.isSubmitted()) {
      final SubmittedReportInfo info = message.getSubmissionInfo();
      url = getUrl(info);
      appendSubmissionInformation(info, text, url);
      text.append(". ");
    }
    else if (message.isSubmitting()) {
      text.append(" Submitting...");
    }
    else if (!message.isRead()) {
      text.append(" ").append(DiagnosticBundle.message("error.list.message.unread"));
    }
    myInfoLabel.setHtmlText(XmlStringUtil.wrapInHtml(text));
    myInfoLabel.setHyperlinkTarget(url);
  }

  public static void appendSubmissionInformation(SubmittedReportInfo info, StringBuilder out, @Nullable String url) {
    if (info.getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED) {
      out.append(" ").append(DiagnosticBundle.message("error.list.message.submission.failed"));
    }
    else {
      if (info.getLinkText() != null) {
        out.append(" ").append(DiagnosticBundle.message("error.list.message.submitted.as.link", url, info.getLinkText()));
        if (info.getStatus() == SubmittedReportInfo.SubmissionStatus.DUPLICATE) {
          out.append(" ").append(DiagnosticBundle.message("error.list.message.duplicate"));
        }
      }
      else {
        out.append(DiagnosticBundle.message("error.list.message.submitted"));
      }
    }
  }

  @Nullable
  public static String getUrl(SubmittedReportInfo info) {
    if (info.getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED || info.getLinkText() == null) {
      return null;
    }
    return info.getURL();
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
            myForeignPluginWarningLabel.setText(DiagnosticBundle.message("error.dialog.foreign.plugin.warning.text"));
          }
          else {
            myForeignPluginWarningLabel.setHyperlinkText(DiagnosticBundle.message("error.dialog.foreign.plugin.warning.text.vendor") + " ", contactInfo, ".");
            myForeignPluginWarningLabel.setHyperlinkTarget(contactInfo);
          }
        }
        else {
          if (StringUtil.isEmpty(contactInfo)) {
            myForeignPluginWarningLabel.setText(DiagnosticBundle.message("error.dialog.foreign.plugin.warning.text.vendor") + " " + vendor + ".");
          }
          else {
            myForeignPluginWarningLabel.setHyperlinkText(DiagnosticBundle.message("error.dialog.foreign.plugin.warning.text.vendor") + " " + vendor + " (", contactInfo, ").");
            myForeignPluginWarningLabel.setHyperlinkTarget(contactInfo);
          }
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

      List<Attachment> attachments = message instanceof LogMessageEx ? ((LogMessageEx)message).getAttachments() : Collections.<Attachment>emptyList();
      if (!attachments.isEmpty()) {
        if (myTabs.indexOfComponent(myAttachmentsTabForm.getContentPane()) == -1) {
          myTabs.addTab(DiagnosticBundle.message("error.attachments.tab.title"), myAttachmentsTabForm.getContentPane());
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
    if (throwable instanceof MessagePool.TooManyErrorsException) {
      return throwable.getMessage();
    }
    else {
      return message.getMessage() + "\n" + message.getThrowableText();
    }
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
    if (idx < 0 || idx >= myMergedMessages.size()) return null;
    return myMergedMessages.get(idx).get(0);
  }

  @Nonnull
  public static Set<PluginId> findAllPluginIds(@Nonnull Throwable t) {
    if (t instanceof PluginException) {
      PluginId pluginId = ((PluginException)t).getPluginId();
      return Set.of(pluginId);
    }

    if(t instanceof ExtensionException) {
      Class extensionClass = ((ExtensionException)t).getExtensionClass();
      PluginId pluginId = consulo.container.plugin.PluginManager.getPluginId(extensionClass);
      if (pluginId == null) {
        LOG.error("There no plugin for extension class: " + extensionClass);
        return Set.of();
      }
      return Set.of(pluginId);
    }

    Set<PluginId> pluginIds = new TreeSet<>();

    for (StackTraceElement element : t.getStackTrace()) {
      String classLoaderName = element.getClassLoaderName();
      if(classLoaderName == null) {
        continue;
      }

      PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(PluginId.getId(classLoaderName));
      if(plugin == null) {
        continue;
      }
      pluginIds.add(plugin.getPluginId());
    }
    return pluginIds;
  }

  @Nullable
  public static PluginId findFirstPluginId(@Nonnull Throwable t) {
    Set<PluginId> pluginIds = findAllPluginIds(t);
    return pluginIds.stream().filter(pluginId -> !PluginIds.isPlatformPlugin(pluginId)).findFirst().orElse(null);
  }

  private class ClearFatalsAction extends AbstractAction {
    protected ClearFatalsAction() {
      super(DiagnosticBundle.message("error.dialog.clear.action"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      myMessagePool.clearFatals();
      doOKAction();
    }

    public void update() {
      putValue(NAME, DiagnosticBundle.message(myMergedMessages.size() > 1 ? "error.dialog.clear.all.action" : "error.dialog.clear.action"));
      setEnabled(!myMergedMessages.isEmpty());
    }
  }

  private class BlameAction extends AbstractAction {
    protected BlameAction() {
      super(DiagnosticBundle.message("error.report.to.consulo.action"));
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
          IdeFrame ideFrame = DesktopIdeFrameUtil.findIdeFrameFromParent(getContentPane());
          parentComponent = ideFrame.getComponent();
        }
        else {
          parentComponent = getContentPane();
        }
        return submitter.trySubmitAsync(getEvents(logMessage), logMessage.getAdditionalInfo(), parentComponent, submittedReportInfo -> {
          logMessage.setSubmitting(false);
          logMessage.setSubmitted(submittedReportInfo);
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              if (!dialogClosed) {
                updateOnSubmit();
              }
            }
          });
        });
      }
      return false;
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

  @Override
  public void calcData(Key<?> key, DataSink sink) {
    if (CURRENT_TRACE_KEY == key) {
      final AbstractMessage message = getSelectedMessage();
      if (message != null) {
        sink.put(CURRENT_TRACE_KEY, getDetailsText(message));
      }
    }
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
    PropertiesComponent.getInstance().setValue(ACTIVE_TAB_OPTION, String.valueOf(myTabs.getSelectedIndex()));
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
      return md5(StringUtil.getThrowableText(exception), "stack-trace");
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
    public void actionPerformed(ActionEvent e) {
      final DataContext dataContext = ((BaseDataManager)DataManager.getInstance()).getDataContextTest((Component)e.getSource());

      AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, myAnalyze.getTemplatePresentation(), ActionManager.getInstance(), e.getModifiers());

      final Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        myAnalyze.actionPerformed(event);
        doOKAction();
      }
    }
  }
}
