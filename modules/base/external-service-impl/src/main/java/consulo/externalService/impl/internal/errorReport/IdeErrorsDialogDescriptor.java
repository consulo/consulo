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

import consulo.application.Application;
import consulo.application.internal.MessagePool;
import consulo.application.internal.MessagePoolListener;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.impl.internal.auth.ExternalServiceLoginService;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.internal.AbstractMessage;
import consulo.logging.internal.GroupedLogMessage;
import consulo.logging.internal.IdeaLoggingEvent;
import consulo.logging.internal.LogMessage;
import consulo.logging.internal.LogMessageEx;
import consulo.logging.internal.SubmittedReportInfo;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.CheckBoxTree;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextArea;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class IdeErrorsDialogDescriptor extends DialogDescriptor implements MessagePoolListener {
    private static final Logger LOG = Logger.getInstance(IdeErrorsDialogDescriptor.class);

    private static final Object STACK_TRACE = new Object();

    private static final int ATTACHMENT_LIST_PROPORTION = 25;

    private class ClearAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ClearAction() {
            super(ExternalServiceLocalize.errorDialogClearAction());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myMessagePool.clearFatals();
            close();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(
                myClearAll ? ExternalServiceLocalize.errorDialogClearAllAction() : ExternalServiceLocalize.errorDialogClearAction()
            );
            e.getPresentation().setEnabled(myCanClear);
        }
    }

    private class ReportAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ReportAction() {
            super(ExternalServiceLocalize.errorReportToConsuloAction());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            report();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myCanReport);
        }
    }

    private static class CloseAction extends DialogCancelAction {
        private CloseAction() {
            getTemplatePresentation().setText(CommonLocalize.closeActionName());
        }
    }

    private final MessagePool myMessagePool;
    private final @Nullable Project myProject;
    private final @Nullable LogMessage myDefaultMessage;
    private final Runnable myOnSubmit;
    private final Runnable myOnClose;
    private final boolean myAssigneeVisible;

    private final List<List<AbstractMessage>> myMergedMessages = new ArrayList<>();
    private List<AbstractMessage> myRawMessages = List.of();
    private int myIndex;

    private @Nullable Dialog myDialog;
    private @Nullable UIAccess myUIAccess;
    private boolean myClosed;
    private boolean myMute;

    private volatile boolean myCanClear;
    private volatile boolean myClearAll;
    private volatile boolean myCanReport;

    private Button myBackButton;
    private Button myForwardButton;
    private Label myCountLabel;
    private HtmlLabel myInfoLabel;
    private HtmlLabel myForeignPluginLabel;
    private TextArea myCommentArea;
    private DockLayout myDetailsHolder;
    private TextArea myContentArea;
    private @Nullable CheckBoxTree<Object> myAttachmentTree;
    private @Nullable TwoComponentSplitLayout myAttachmentSplit;
    private @Nullable Disposable myAttachmentTreeDisposable;
    private @Nullable List<Attachment> myShownAttachments;
    private HtmlLabel myAttachmentWarningLabel;
    private HtmlLabel myCredentialsLabel;
    private HorizontalLayout myAssigneeRow;
    private MutableFlatDataModel<Developer> myDevelopers;
    private ComboBox<Developer> myAssigneeComboBox;

    @RequiredUIAccess
    public static Dialog show(
        DialogService dialogService,
        MessagePool messagePool,
        @Nullable Project project,
        @Nullable LogMessage defaultMessage,
        @RequiredUIAccess Runnable onSubmit,
        @RequiredUIAccess Runnable onClose
    ) {
        IdeErrorsDialogDescriptor descriptor = new IdeErrorsDialogDescriptor(messagePool, project, defaultMessage, onSubmit, onClose);
        Dialog dialog = project != null ? dialogService.build(project, descriptor) : dialogService.build(descriptor);
        descriptor.myDialog = dialog;
        dialog.showAsync();
        return dialog;
    }

    private IdeErrorsDialogDescriptor(
        MessagePool messagePool,
        @Nullable Project project,
        @Nullable LogMessage defaultMessage,
        Runnable onSubmit,
        Runnable onClose
    ) {
        super(ExternalServiceLocalize.errorListTitle());
        myMessagePool = messagePool;
        myProject = project;
        myDefaultMessage = defaultMessage;
        myOnSubmit = onSubmit;
        myOnClose = onClose;
        myAssigneeVisible = getConfiguration().getState(ExternalService.DEVELOPER_LIST) != ThreeState.NO;
    }

    @Override
    public boolean isModal() {
        return false;
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "IDE.errors.dialog";
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(50, 34);
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        List<AnAction> actions = new ArrayList<>();
        if (inverseOrder) {
            actions.add(new CloseAction());
        }
        actions.add(new ClearAction());
        if (getConfiguration().getState(ExternalService.ERROR_REPORTING) != ThreeState.NO) {
            actions.add(new ReportAction());
        }
        if (!inverseOrder) {
            actions.add(new CloseAction());
        }
        return actions.toArray(AnAction[]::new);
    }

    @Override
    public boolean isDefaultAction(AnAction action) {
        return action instanceof CloseAction;
    }

    @Override
    @RequiredUIAccess
    public Component getPreferredFocusedComponent() {
        return myCommentArea;
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        myUIAccess = UIAccess.current();

        myBackButton = navigationButton(PlatformIconGroup.actionsBack(), ExternalServiceLocalize.errorListBackAction(), -1);
        myForwardButton = navigationButton(PlatformIconGroup.actionsForward(), ExternalServiceLocalize.errorListForwardAction(), 1);
        myCountLabel = Label.create();

        myInfoLabel = HtmlLabel.create(LocalizeValue.empty());
        myInfoLabel.addHyperlinkListener(IdeErrorsDialogDescriptor::openLink);

        myForeignPluginLabel = HtmlLabel.create(LocalizeValue.empty());
        myForeignPluginLabel.setImage(PlatformIconGroup.generalWarning());
        myForeignPluginLabel.addHyperlinkListener(IdeErrorsDialogDescriptor::openLink);

        HorizontalLayout navigation = HorizontalLayout.create(Space.X_SMALL);
        navigation.add(myBackButton);
        navigation.add(myForwardButton);
        navigation.add(myCountLabel);

        DockLayout headerRow = DockLayout.create(Space.MEDIUM);
        headerRow.left(navigation);
        headerRow.center(myInfoLabel);

        VerticalLayout header = VerticalLayout.create(Space.SMALL);
        header.add(headerRow);
        header.add(myForeignPluginLabel);

        myCommentArea = TextArea.create();
        myCommentArea.setPlaceholder(ExternalServiceLocalize.errorDialogCommentPrompt());
        myCommentArea.setMinRows(4);
        myCommentArea.setMaxRows(4);
        myCommentArea.addValueListener(event -> {
            AbstractMessage message = getSelectedMessage();
            if (!myMute && message != null) {
                message.setAdditionalInfo(StringUtil.notNullize(event.getValue()).trim());
            }
        });

        myContentArea = TextArea.create();
        myContentArea.setEditable(false);

        myDetailsHolder = DockLayout.create();

        DockLayout body = DockLayout.create(Space.SMALL);
        body.top(myCommentArea);
        body.center(myDetailsHolder);

        myAttachmentWarningLabel = HtmlLabel.create(LocalizeValue.empty());
        myAttachmentWarningLabel.setImage(PlatformIconGroup.generalBalloonwarning());
        myAttachmentWarningLabel.addHyperlinkListener(event -> selectFirstIncludedAttachment());

        myCredentialsLabel = HtmlLabel.create(LocalizeValue.empty());
        myCredentialsLabel.addHyperlinkListener(event -> login());

        myDevelopers = FlatDataModel.of(List.of());
        myAssigneeComboBox = ComboBox.create(myDevelopers);
        myAssigneeComboBox.setTextRenderer(developer -> developer == null
            ? LocalizeValue.empty()
            : LocalizeValue.of(developer.getDisplayText()));
        myAssigneeComboBox.addValueListener(event -> {
            AbstractMessage message = getSelectedMessage();
            Developer developer = event.getValue();
            if (!myMute && message != null) {
                message.setAssigneeId(developer == null ? 0 : developer.getId());
            }
        });

        Label assigneeLabel = Label.create(ExternalServiceLocalize.errorDialogAssigneeLabel());
        assigneeLabel.setTarget(myAssigneeComboBox);

        myAssigneeRow = HorizontalLayout.create(Space.SMALL);
        myAssigneeRow.add(assigneeLabel);
        myAssigneeRow.add(myAssigneeComboBox);
        myAssigneeRow.setVisible(myAssigneeVisible);

        VerticalLayout footer = VerticalLayout.create(Space.SMALL);
        footer.add(myAttachmentWarningLabel);
        footer.add(myAssigneeRow);
        footer.add(myCredentialsLabel);

        DockLayout root = DockLayout.create(Space.MEDIUM);
        root.top(header);
        root.center(body);
        root.bottom(footer);

        Disposer.register(uiDisposable, this::onDispose);

        myMessagePool.addListener(this);

        rebuildHeaders();
        if (myDefaultMessage == null || !moveSelectionToMessage(myDefaultMessage)) {
            moveSelectionToEarliestMessage();
        }

        if (myAssigneeVisible) {
            loadDevelopers();
        }
        return root;
    }

    @RequiredUIAccess
    private Button navigationButton(Image icon, LocalizeValue tooltip, int step) {
        Button button = Button.create(LocalizeValue.empty(), event -> {
            myIndex += step;
            updateControls();
        });
        button.setIcon(icon);
        button.addStyle(ButtonStyle.TOOLBAR);
        button.setToolTipText(tooltip);
        return button;
    }

    @RequiredUIAccess
    private void onDispose() {
        myClosed = true;
        myMessagePool.removeListener(this);
        disposeAttachmentTree();
        markAllAsRead();
        myOnClose.run();
    }

    @RequiredUIAccess
    private void close() {
        Dialog dialog = myDialog;
        if (dialog != null && !myClosed) {
            dialog.doCancelAction();
        }
    }

    @Override
    public void newEntryAdded() {
        onUIThread(() -> {
            rebuildHeaders();
            updateControls();
        });
    }

    @Override
    public void poolCleared() {
        onUIThread(this::close);
    }

    @Override
    public void entryWasRead() {
    }

    private void onUIThread(@RequiredUIAccess Runnable runnable) {
        UIAccess uiAccess = myUIAccess;
        if (uiAccess == null) {
            return;
        }

        uiAccess.give(() -> {
            if (!myClosed) {
                runnable.run();
            }
        });
    }

    private void loadDevelopers() {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> {
                try {
                    return DevelopersLoader.fetchDevelopers();
                }
                catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((developers, error) -> {
                if (error != null) {
                    NotificationService.getInstance()
                        .newWarn(ReportMessages.GROUP)
                        .title(ExternalServiceLocalize.errorDialogDevelopersErrorTitle())
                        .content(ExternalServiceLocalize.errorDialogDevelopersErrorText())
                        .notify(myProject);
                    return;
                }

                if (myClosed) {
                    return;
                }

                myMute = true;
                try {
                    myDevelopers.replaceAll(developers);
                }
                finally {
                    myMute = false;
                }
                updateAssignee(getSelectedMessage());
            }, uiAccess);
    }

    @RequiredUIAccess
    private boolean moveSelectionToMessage(LogMessage defaultMessage) {
        for (int i = 0; i < myMergedMessages.size(); i++) {
            if (myMergedMessages.get(i).contains(defaultMessage)) {
                myIndex = i;
                updateControls();
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void moveSelectionToEarliestMessage() {
        myIndex = 0;
        for (int i = 0; i < myMergedMessages.size(); i++) {
            if (!getMessageAt(i).isRead()) {
                myIndex = i;
                break;
            }
        }

        updateControls();
    }

    @RequiredUIAccess
    private void updateControls() {
        myIndex = Math.max(0, Math.min(myIndex, myMergedMessages.size() - 1));

        AbstractMessage message = getSelectedMessage();

        myBackButton.setEnabled(myIndex > 0);
        myForwardButton.setEnabled(myIndex < myMergedMessages.size() - 1);
        myCountLabel.setText(
            myMergedMessages.isEmpty()
                ? ExternalServiceLocalize.errorListEmpty()
                : ExternalServiceLocalize.errorListMessageIndexCount(Integer.toString(myIndex + 1), myMergedMessages.size())
        );

        updateInfoLabel(message);
        updateForeignPluginLabel(message);
        updateCredentials(message);
        updateComment(message);
        updateDetails(message);
        updateAttachmentWarning(message);
        updateAssignee(message);

        myCanClear = !myMergedMessages.isEmpty();
        myClearAll = myMergedMessages.size() > 1;
        myCanReport = message != null && !message.isSubmitting() && getSubmitter(message.getThrowable()) != null;
        updateOkButtonState();
    }

    @RequiredUIAccess
    private void updateInfoLabel(@Nullable AbstractMessage message) {
        if (message == null || message.getThrowable() instanceof MessagePool.TooManyErrorsException) {
            myInfoLabel.setText(LocalizeValue.empty());
            return;
        }

        Throwable throwable = message.getThrowable();
        StringBuilder text = new StringBuilder();
        PluginId pluginId = PluginExceptionUtil.findFirstPluginId(throwable);
        PluginDescriptor plugin = pluginId == null ? null : PluginManager.findPlugin(pluginId);
        if (plugin != null) {
            text.append(ExternalServiceLocalize.errorListMessageBlamePlugin(XmlStringUtil.escapeText(plugin.getName())));
        }
        else if (throwable instanceof AbstractMethodError) {
            text.append(ExternalServiceLocalize.errorListMessageBlameUnknownPlugin());
        }
        else {
            text.append(ExternalServiceLocalize.errorListMessageBlameCore());
        }
        text.append(" ").append(ExternalServiceLocalize.errorListMessageInfo(
            DateFormatUtil.formatPrettyDateTime(message.getDate()),
            myMergedMessages.get(myIndex).size()
        ));

        SubmittedReportInfo info = message.getSubmissionInfo();
        if (message.isSubmitted() && info != null) {
            SubmittedReportInfoUtil.appendSubmissionInformation(info, text, SubmittedReportInfo.getUrl(info));
            text.append(".");
        }
        else if (message.isSubmitting()) {
            text.append(" ").append(ExternalServiceLocalize.errorListMessageSubmitting());
        }
        else if (!message.isRead()) {
            text.append(" ").append(ExternalServiceLocalize.errorListMessageUnread());
        }
        myInfoLabel.setText(LocalizeValue.of(text.toString()));
    }

    @RequiredUIAccess
    private void updateForeignPluginLabel(@Nullable AbstractMessage message) {
        PluginDescriptor plugin = null;
        if (message != null && getSubmitter(message.getThrowable()) == null) {
            PluginId pluginId = PluginExceptionUtil.findFirstPluginId(message.getThrowable());
            plugin = pluginId == null ? null : PluginManager.findPlugin(pluginId);
        }

        if (plugin == null) {
            myForeignPluginLabel.setVisible(false);
            return;
        }

        String vendor = plugin.getVendor();
        String contact = plugin.getVendorUrl();
        if (StringUtil.isEmpty(contact)) {
            contact = plugin.getVendorEmail();
        }

        String text;
        if (StringUtil.isEmpty(vendor) && StringUtil.isEmpty(contact)) {
            text = ExternalServiceLocalize.errorDialogForeignPluginWarningText().get();
        }
        else {
            StringBuilder builder = new StringBuilder(ExternalServiceLocalize.errorDialogForeignPluginWarningTextVendor().get());
            if (!StringUtil.isEmpty(vendor)) {
                builder.append(" ").append(XmlStringUtil.escapeText(vendor));
            }
            if (!StringUtil.isEmpty(contact)) {
                String link = "<a href=\"" + XmlStringUtil.escapeText(contactLink(contact)) + "\">" + XmlStringUtil.escapeText(contact) + "</a>";
                builder.append(StringUtil.isEmpty(vendor) ? " " + link : " (" + link + ")");
            }
            text = builder.append(".").toString();
        }

        myForeignPluginLabel.setText(LocalizeValue.of(text));
        myForeignPluginLabel.setVisible(true);
    }

    @RequiredUIAccess
    private void updateCredentials(@Nullable AbstractMessage message) {
        boolean visible = message != null && getSubmitter(message.getThrowable()) instanceof ITNReporter;
        myCredentialsLabel.setVisible(visible);
        if (!visible) {
            return;
        }

        String email = getConfiguration().getEmail();
        LocalizeValue text = email == null
            ? ExternalServiceLocalize.diagnosticErrorReportSubmitErrorAnonymously()
            : ExternalServiceLocalize.diagnosticErrorReportSubmitReportAs(XmlStringUtil.escapeText(email));
        myCredentialsLabel.setText(text.map(XmlStringUtil::stripHtml));
    }

    @RequiredUIAccess
    private void login() {
        if (getConfiguration().getEmail() != null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        Application.get().getInstance(ExternalServiceLoginService.class).login().whenCompleteAsync((userName, error) -> {
            if (!myClosed) {
                updateCredentials(getSelectedMessage());
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void updateComment(@Nullable AbstractMessage message) {
        myMute = true;
        try {
            myCommentArea.setValue(message == null ? "" : StringUtil.notNullize(message.getAdditionalInfo()));
            myCommentArea.setEnabled(message != null);
        }
        finally {
            myMute = false;
        }
    }

    @RequiredUIAccess
    private void updateDetails(@Nullable AbstractMessage message) {
        List<Attachment> attachments = message instanceof LogMessageEx logMessageEx ? logMessageEx.getAttachments() : List.of();

        myContentArea.setValue(message == null ? "" : getDetailsText(message));

        if (attachments.isEmpty()) {
            disposeAttachmentTree();
            myShownAttachments = null;
            myDetailsHolder.center(myContentArea);
            return;
        }

        if (attachments.equals(myShownAttachments) && myAttachmentTree != null) {
            return;
        }

        disposeAttachmentTree();
        myShownAttachments = attachments;

        CheckBoxTree<Object> tree = CheckBoxTree.create((nodeFactory, parentValue) -> {
            if (parentValue != null) {
                return;
            }

            TreeNode<Object> stackTrace = nodeFactory.apply(STACK_TRACE);
            stackTrace.setLeaf(true);
            stackTrace.setRenderer((value, presentation) -> presentation.append(ExternalServiceLocalize.errorDialogStacktrace()));

            for (Attachment attachment : attachments) {
                TreeNode<Object> node = nodeFactory.apply(attachment);
                node.setLeaf(true);
                node.setRenderer((value, presentation) -> presentation.append(LocalizeValue.of(attachment.getName())));
            }
        });

        Disposable treeDisposable = Disposable.newDisposable();
        Disposer.register(treeDisposable, tree.destroyHook());
        myAttachmentTreeDisposable = treeDisposable;
        myAttachmentTree = tree;

        tree.setCheckEnabledFilter(value -> value != STACK_TRACE);
        tree.setChecked(STACK_TRACE, true);
        for (Attachment attachment : attachments) {
            tree.setChecked(attachment, attachment.isIncluded());
        }
        tree.addCheckListener(event -> {
            if (event.getValue() instanceof Attachment attachment) {
                attachment.setIncluded(event.isChecked());
                updateAttachmentWarning(getSelectedMessage());
            }
        });
        tree.addSelectListener(event -> {
            TreeNode<Object> node = event.getValue();
            Object value = node == null ? null : node.getValue();
            AbstractMessage selected = getSelectedMessage();
            if (value instanceof Attachment attachment) {
                myContentArea.setValue(attachment.getDisplayText());
            }
            else {
                myContentArea.setValue(selected == null ? "" : getDetailsText(selected));
            }
        });

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(ScrollableLayout.create(tree));
        split.setSecondComponent(myContentArea);
        split.setProportion(ATTACHMENT_LIST_PROPORTION);
        myAttachmentSplit = split;
        myDetailsHolder.center(split);
    }

    @RequiredUIAccess
    private void disposeAttachmentTree() {
        TwoComponentSplitLayout split = myAttachmentSplit;
        myAttachmentSplit = null;
        if (split != null) {
            split.setSecondComponent(null);
        }

        Disposable disposable = myAttachmentTreeDisposable;
        myAttachmentTreeDisposable = null;
        myAttachmentTree = null;
        if (disposable != null) {
            Disposer.dispose(disposable);
        }
    }

    @RequiredUIAccess
    private void updateAttachmentWarning(@Nullable AbstractMessage message) {
        List<Attachment> included = message instanceof LogMessageEx logMessageEx
            ? ContainerUtil.filter(logMessageEx.getAttachments(), Attachment::isIncluded)
            : List.of();

        if (included.isEmpty()) {
            myAttachmentWarningLabel.setVisible(false);
            return;
        }

        myAttachmentWarningLabel.setText(
            included.size() == 1
                ? ExternalServiceLocalize.diagnosticErrorReportIncludeAttachmentWarning(XmlStringUtil.escapeText(included.get(0).getName()))
                : ExternalServiceLocalize.diagnosticErrorReportIncludeAttachmentsWarning(included.size())
        );
        myAttachmentWarningLabel.setVisible(true);
    }

    @RequiredUIAccess
    private void updateAssignee(@Nullable AbstractMessage message) {
        if (!myAssigneeVisible) {
            return;
        }

        long assigneeId = message == null ? 0 : message.getAssigneeId();
        myMute = true;
        try {
            for (int i = 0; i < myDevelopers.getSize(); i++) {
                Developer developer = myDevelopers.get(i);
                if (developer.getId() == assigneeId) {
                    myAssigneeComboBox.setValue(developer);
                    break;
                }
            }
        }
        finally {
            myMute = false;
        }
        myAssigneeComboBox.setEnabled(message != null && myDevelopers.getSize() > 0);
    }

    @RequiredUIAccess
    private void selectFirstIncludedAttachment() {
        CheckBoxTree<Object> tree = myAttachmentTree;
        TreeNode<Object> root = tree == null ? null : tree.getRootNode();
        if (root == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        root.findChildDeep(value -> value instanceof Attachment attachment && attachment.isIncluded())
            .whenCompleteAsync((node, error) -> {
                if (node != null && tree == myAttachmentTree) {
                    tree.select(node);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void report() {
        AbstractMessage message = getSelectedMessage();
        if (message == null) {
            return;
        }

        ErrorReportSubmitter submitter = getSubmitter(message.getThrowable());
        if (submitter == null) {
            return;
        }

        message.setAdditionalInfo(StringUtil.notNullize(myCommentArea.getValue()).trim());

        boolean closeDialog = myMergedMessages.size() == 1;

        message.setSubmitting(true);

        UIAccess uiAccess = UIAccess.current();
        boolean started = submitter.trySubmitAsync(getEvents(message), StringUtil.notNullize(message.getAdditionalInfo()), myProject, info -> {
            message.setSubmitting(false);
            message.setSubmitted(info);
            uiAccess.give(() -> {
                myOnSubmit.run();
                if (!myClosed) {
                    updateControls();
                }
            });
        });

        if (closeDialog && started) {
            close();
        }
        else {
            if (!started) {
                message.setSubmitting(false);
            }
            rebuildHeaders();
            updateControls();
        }
    }

    private static IdeaLoggingEvent[] getEvents(AbstractMessage message) {
        if (message instanceof GroupedLogMessage groupedLogMessage) {
            List<AbstractMessage> messages = groupedLogMessage.getMessages();
            IdeaLoggingEvent[] events = new IdeaLoggingEvent[messages.size()];
            for (int i = 0; i < events.length; i++) {
                events[i] = getEvent(messages.get(i));
            }
            return events;
        }
        return new IdeaLoggingEvent[]{getEvent(message)};
    }

    private static IdeaLoggingEvent getEvent(AbstractMessage message) {
        if (message instanceof LogMessageEx logMessageEx) {
            return logMessageEx.toEvent();
        }
        return new IdeaLoggingEvent(message.getMessage(), message.getThrowable()) {
            @Override
            public AbstractMessage getData() {
                return message;
            }
        };
    }

    private void rebuildHeaders() {
        myMergedMessages.clear();
        myRawMessages = myMessagePool.getFatalErrors(true, true);

        Map<String, List<AbstractMessage>> hashToMessages = new LinkedHashMap<>();
        for (AbstractMessage message : myRawMessages) {
            hashToMessages.computeIfAbsent(getThrowableHashCode(message.getThrowable()), key -> new ArrayList<>()).add(0, message);
        }
        myMergedMessages.addAll(hashToMessages.values());
    }

    private void markAllAsRead() {
        for (AbstractMessage message : myRawMessages) {
            message.setRead(true);
        }
    }

    private @Nullable AbstractMessage getSelectedMessage() {
        return getMessageAt(myIndex);
    }

    private @Nullable AbstractMessage getMessageAt(int index) {
        if (index < 0 || index >= myMergedMessages.size()) {
            return null;
        }
        return myMergedMessages.get(index).get(0);
    }

    private static String getDetailsText(AbstractMessage message) {
        Throwable throwable = message.getThrowable();
        return throwable instanceof MessagePool.TooManyErrorsException
            ? throwable.getMessage()
            : message.getMessage() + "\n" + message.getThrowableText();
    }

    public static @Nullable ErrorReportSubmitter getSubmitter(Throwable throwable) {
        if (throwable instanceof MessagePool.TooManyErrorsException || throwable instanceof AbstractMethodError) {
            return null;
        }
        return ITNReporter.ourInternalInstance;
    }

    private static ExternalServiceConfiguration getConfiguration() {
        return Application.get().getInstance(ExternalServiceConfiguration.class);
    }

    @RequiredUIAccess
    private static void openLink(HyperlinkEvent event) {
        String href = event.getDescription();
        if (!StringUtil.isEmpty(href)) {
            Platform.current().openInBrowser(href);
        }
    }

    private static String contactLink(String contact) {
        return contact.contains("@") && !contact.contains("://") ? "mailto:" + contact : contact;
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
}
