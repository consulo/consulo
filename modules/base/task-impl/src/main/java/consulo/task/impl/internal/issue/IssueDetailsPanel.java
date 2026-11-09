/*
 * Copyright 2013-2026 consulo.io
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
package consulo.task.impl.internal.issue;

import consulo.accountManagement.Account;
import consulo.application.dumb.DumbAware;
import consulo.application.util.DateFormatUtil;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.CustomTaskState;
import consulo.task.Task;
import consulo.task.TaskRepository;
import consulo.task.impl.internal.action.OpenTaskDialog;
import consulo.task.issue.IssueAssigneeSupport;
import consulo.task.issue.IssueAttachment;
import consulo.task.issue.IssueAttachmentSupport;
import consulo.task.issue.IssueComment;
import consulo.task.issue.IssueCommentSupport;
import consulo.task.issue.IssueDetails;
import consulo.task.issue.IssueField;
import consulo.task.issue.IssueLink;
import consulo.task.issue.IssueLinkSupport;
import consulo.task.issue.IssuePrioritySupport;
import consulo.task.issue.IssueStateSupport;
import consulo.task.issue.IssueTracker;
import consulo.task.issue.IssueWorkLog;
import consulo.task.issue.IssueWorkLogSupport;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.model.FlatDataModel;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class IssueDetailsPanel {
    private enum DetailsTab {
        PREVIEW,
        ATTACHMENTS,
        LINKS,
        SUBTASKS,
        COMMENTS,
        WORK_LOG
    }

    private final Project myProject;
    private final TaskRepository myRepository;
    private final Consumer<Task> myIssueChanged;
    private final DockLayout myRoot = DockLayout.create(Space.NONE);

    private @Nullable Task myTask;
    private int myGeneration;
    private DetailsTab mySelectedTab = DetailsTab.PREVIEW;

    IssueDetailsPanel(Project project, TaskRepository repository, Consumer<Task> issueChanged) {
        myProject = project;
        myRepository = repository;
        myIssueChanged = issueChanged;
    }

    Component getComponent() {
        return myRoot;
    }

    private IssueTracker getTracker() {
        return myRepository.getIssueTracker();
    }

    @RequiredUIAccess
    void show(@Nullable Task task) {
        myTask = task;
        int generation = ++myGeneration;
        if (task == null) {
            setMessage(TaskLocalize.issuesDetailsEmpty());
            return;
        }
        setMessage(TaskLocalize.issuesStatusLoading());
        getTracker().loadDetails(task).whenCompleteAsync((details, error) -> {
            if (generation != myGeneration) {
                return;
            }
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                setMessage(TaskLocalize.issuesStatusError(StringUtil.notNullize(cause.getMessage(), cause.getClass().getSimpleName())));
                return;
            }
            build(details);
        }, UIAccess.current());
    }

    @RequiredUIAccess
    private void setMessage(LocalizeValue text) {
        DockLayout layout = DockLayout.create();
        layout.center(Label.create(text));
        myRoot.center(layout);
    }

    @RequiredUIAccess
    private void build(IssueDetails details) {
        Task task = details.getTask();
        TabbedLayout tabs = TabbedLayout.create(StaticPosition.BOTTOM);
        Map<DetailsTab, Tab> tabMap = new EnumMap<>(DetailsTab.class);

        tabMap.put(DetailsTab.PREVIEW, tabs.addTab(TaskLocalize.issuesTabPreview(), createPreview(details)));
        tabMap.put(DetailsTab.ATTACHMENTS, tabs.addTab(withCount(TaskLocalize.issuesTabAttachments(), details.getAttachments().size()),
            createAttachments(task, details.getAttachments())));
        tabMap.put(DetailsTab.LINKS, tabs.addTab(withCount(TaskLocalize.issuesTabLinks(), details.getLinks().size()),
            createLinks(task, details.getLinks())));
        tabMap.put(DetailsTab.SUBTASKS, tabs.addTab(withCount(TaskLocalize.issuesTabSubtasks(), details.getSubtasks().size()),
            createSubtasks(details.getSubtasks())));
        tabMap.put(DetailsTab.COMMENTS, tabs.addTab(withCount(TaskLocalize.issuesTabComments(), details.getComments().size()),
            createComments(task, details.getComments())));
        tabMap.put(DetailsTab.WORK_LOG, tabs.addTab(withCount(TaskLocalize.issuesTabWorkLog(), details.getWorkLogs().size()),
            createWorkLogs(task, details.getWorkLogs())));

        Tab selected = tabMap.get(mySelectedTab);
        if (selected != null) {
            selected.select();
        }
        tabs.addSelectListener(event -> {
            for (Map.Entry<DetailsTab, Tab> entry : tabMap.entrySet()) {
                if (entry.getValue() == event.getTab()) {
                    mySelectedTab = entry.getKey();
                }
            }
        });
        myRoot.center(tabs);
    }

    private static LocalizeValue withCount(LocalizeValue name, int count) {
        return count > 0 ? TaskLocalize.issuesTabCount(name.get(), count) : name;
    }

    @RequiredUIAccess
    private Component createToolWindowPanel(ActionGroup actions, Component content) {
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar("TaskIssueDetails", actions, ActionToolbar.Style.VERTICAL);
        toolbar.setTargetUIComponent(content);
        DockLayout toolbarColumn = DockLayout.create(Space.NONE);
        toolbarColumn.center(toolbar.getUIComponent());
        toolbarColumn.borderBuilder().rightSet().apply();

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.left(toolbarColumn);
        panel.center(content);
        return panel;
    }

    @RequiredUIAccess
    private void run(CompletableFuture<?> future) {
        Task task = myTask;
        future.whenCompleteAsync((result, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                MessageBoxes.okError(cause).showAsync();
                return;
            }
            if (task != null && task == myTask) {
                show(task);
                myIssueChanged.accept(task);
            }
        }, UIAccess.current());
    }

    @RequiredUIAccess
    private void confirmAndRun(LocalizeValue question, Supplier<CompletableFuture<?>> operation) {
        MessageBoxes.yesNo().text(question).showAsync().whenCompleteAsync((answer, error) -> {
            if (Boolean.TRUE.equals(answer)) {
                run(operation.get());
            }
        }, UIAccess.current());
    }

    @RequiredUIAccess
    private Component createPreview(IssueDetails details) {
        Task task = details.getTask();
        StringBuilder html = new StringBuilder();
        html.append("<p>").append(XmlStringUtil.escapeText(task.getPresentableId()));
        String state = IssueColumn.getStateText(task);
        if (!state.isEmpty()) {
            html.append(" &middot; <b>").append(XmlStringUtil.escapeText(state)).append("</b>");
        }
        html.append("</p><h3>").append(XmlStringUtil.escapeText(task.getSummary())).append("</h3><table>");
        appendRow(html, TaskLocalize.issuesColumnType().get(), IssueColumn.getTypeText(task));
        for (IssueField field : details.getFields()) {
            appendRow(html, field.getName().get(), field.getValue());
        }
        appendRow(html, TaskLocalize.issuesColumnCreated().get(), formatDate(task.getCreated()));
        appendRow(html, TaskLocalize.issuesColumnUpdated().get(), formatDate(task.getUpdated()));
        html.append("</table>");
        String description = task.getDescription();
        if (!StringUtil.isEmptyOrSpaces(description)) {
            html.append("<p><b>").append(XmlStringUtil.escapeText(TaskLocalize.issuesPreviewDescription().get())).append("</b><br/>")
                .append(XmlStringUtil.escapeText(description).replace("\n", "<br/>")).append("</p>");
        }
        HtmlLabel label = HtmlLabel.create(LocalizeValue.of(html.toString()));
        VerticalLayout content = VerticalLayout.create();
        content.add(label);
        content.paddingBuilder().allSet(Space.LARGE).apply();

        DefaultActionGroup group = new DefaultActionGroup();
        IssueStateSupport stateSupport = getTracker().getSupport(IssueStateSupport.class);
        if (stateSupport != null) {
            group.add(new ChooseAction<CustomTaskState>(TaskLocalize.issuesActionTransition(), PlatformIconGroup.actionsForward(),
                () -> stateSupport.getAvailableStates(task).thenApply(List::copyOf),
                CustomTaskState::getPresentableName,
                stateSupport::setState));
        }
        IssueAssigneeSupport assigneeSupport = getTracker().getSupport(IssueAssigneeSupport.class);
        if (assigneeSupport != null) {
            group.add(new ChooseAction<String>(TaskLocalize.issuesActionAssign(), PlatformIconGroup.actionsLoginavatar(),
                () -> assigneeSupport.getAssignableUsers(task),
                Function.identity(),
                assigneeSupport::assign));
        }
        IssuePrioritySupport prioritySupport = getTracker().getSupport(IssuePrioritySupport.class);
        if (prioritySupport != null) {
            group.add(new ChooseAction<String>(TaskLocalize.issuesActionPriority(), PlatformIconGroup.generalArrowup(),
                () -> prioritySupport.getPriorities(task),
                Function.identity(),
                prioritySupport::setPriority));
        }
        group.addSeparator();
        group.add(new SimpleAction(TaskLocalize.issuesActionStartWorking(), PlatformIconGroup.actionsExecute(),
            () -> true, () -> OpenTaskDialog.show(myProject, task)));
        group.add(new SimpleAction(TaskLocalize.issuesActionOpenInBrowser(), PlatformIconGroup.ideExternallink(),
            () -> task.getIssueUrl() != null, () -> openInBrowser(task.getIssueUrl())));
        return createToolWindowPanel(group, ScrollableLayout.create(content));
    }

    private static void appendRow(StringBuilder builder, String name, String value) {
        if (!value.isEmpty()) {
            builder.append("<tr><td><b>").append(XmlStringUtil.escapeText(name)).append("</b>&nbsp;&nbsp;</td><td>")
                .append(XmlStringUtil.escapeText(value)).append("</td></tr>");
        }
    }

    private static String formatDate(@Nullable Date date) {
        return date == null ? "" : DateFormatUtil.formatPrettyDateTime(date);
    }

    private static void openInBrowser(@Nullable String url) {
        if (url != null) {
            Platform.current().openInBrowser(url);
        }
    }

    @RequiredUIAccess
    private Component createAttachments(Task task, List<IssueAttachment> attachments) {
        ListBox<IssueAttachment> list = ListBox.create(FlatDataModel.of(attachments));
        list.setRenderer((presentation, item) -> {
            IssueAttachment value = item.getValue();
            if (value != null) {
                presentation.withIcon(PlatformIconGroup.filetypesAny_type());
                presentation.append(value.getName());
                presentation.append("  " + StringUtil.formatFileSize(value.getSize()), TextAttribute.GRAYED);
            }
        });
        IssueAttachmentSupport support = getTracker().getSupport(IssueAttachmentSupport.class);
        DefaultActionGroup group = new DefaultActionGroup();
        if (support != null) {
            group.add(new SimpleAction(TaskLocalize.issuesActionAttachmentAdd(), PlatformIconGroup.generalAdd(), () -> true, () ->
                FileChooser.choosePath(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), myProject, null)
                    .whenCompleteAsync((path, error) -> {
                        if (path != null) {
                            run(support.addAttachment(task, path));
                        }
                    }, UIAccess.current())));
            group.add(new SimpleAction(TaskLocalize.issuesActionAttachmentDelete(), PlatformIconGroup.generalRemove(),
                () -> list.getValue() != null,
                () -> {
                    IssueAttachment selected = list.getValue();
                    if (selected != null) {
                        confirmAndRun(TaskLocalize.issuesConfirmDelete(selected.getName()), () -> support.deleteAttachment(task, selected));
                    }
                }));
        }
        group.add(new SimpleAction(TaskLocalize.issuesActionOpenInBrowser(), PlatformIconGroup.ideExternallink(),
            () -> list.getValue() != null && list.getValue().getUrl() != null,
            () -> {
                IssueAttachment selected = list.getValue();
                openInBrowser(selected == null ? null : selected.getUrl());
            }));
        return createToolWindowPanel(group, ScrollableLayout.create(list));
    }

    @RequiredUIAccess
    private Component createLinks(Task task, List<IssueLink> links) {
        ListBox<IssueLink> list = ListBox.create(FlatDataModel.of(links));
        list.setRenderer((presentation, item) -> {
            IssueLink value = item.getValue();
            if (value != null) {
                presentation.append(value.getType() + " ", TextAttribute.GRAYED);
                presentation.append(value.getIssueId(), TextAttribute.REGULAR_BOLD);
                presentation.append(" " + value.getIssueSummary());
            }
        });
        IssueLinkSupport support = getTracker().getSupport(IssueLinkSupport.class);
        DefaultActionGroup group = new DefaultActionGroup();
        if (support != null) {
            group.add(new SimpleAction(TaskLocalize.issuesActionLinkAdd(), PlatformIconGroup.generalAdd(), () -> true, () -> {
                UIAccess uiAccess = UIAccess.current();
                support.getLinkTypes(task).whenCompleteAsync((types, error) -> {
                    if (types != null) {
                        IssueLinkDialogDescriptor.showAsync(types).thenAccept(result -> {
                            if (result != null) {
                                run(support.addLink(task, result.type(), result.issueId()));
                            }
                        });
                    }
                }, uiAccess);
            }));
            group.add(new SimpleAction(TaskLocalize.issuesActionLinkDelete(), PlatformIconGroup.generalRemove(),
                () -> list.getValue() != null,
                () -> {
                    IssueLink selected = list.getValue();
                    if (selected != null) {
                        confirmAndRun(TaskLocalize.issuesConfirmDelete(selected.getIssueId()), () -> support.deleteLink(task, selected));
                    }
                }));
        }
        return createToolWindowPanel(group, ScrollableLayout.create(list));
    }

    @RequiredUIAccess
    private Component createSubtasks(List<Task> subtasks) {
        ListBox<Task> list = ListBox.create(FlatDataModel.of(subtasks));
        list.setRenderer((presentation, item) -> {
            Task value = item.getValue();
            if (value != null) {
                presentation.withIcon(value.getIcon());
                presentation.append(value.getPresentableId(), TextAttribute.REGULAR_BOLD);
                presentation.append(" " + value.getSummary());
                String state = IssueColumn.getStateText(value);
                if (!state.isEmpty()) {
                    presentation.append("  " + state, TextAttribute.GRAYED);
                }
            }
        });
        list.addDoubleClickListener(event -> {
            Task selected = list.getValue();
            if (selected != null) {
                OpenTaskDialog.show(myProject, selected);
            }
        });
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new SimpleAction(TaskLocalize.issuesActionStartWorking(), PlatformIconGroup.actionsExecute(),
            () -> list.getValue() != null,
            () -> {
                Task selected = list.getValue();
                if (selected != null) {
                    OpenTaskDialog.show(myProject, selected);
                }
            }));
        return createToolWindowPanel(group, ScrollableLayout.create(list));
    }

    @RequiredUIAccess
    private Component createComments(Task task, List<IssueComment> comments) {
        MessageList messages = MessageList.create();
        messages.setMarkdown(true);
        messages.setShowOwnAvatar(true);
        IssueCommentSupport support = getTracker().getSupport(IssueCommentSupport.class);
        String own = getOwnName();
        for (IssueComment comment : comments) {
            String author = StringUtil.notNullize(comment.getAuthor());
            MessageListItem item = messages.add(LocalizeValue.of(author), comment.getText());
            Date date = comment.getDate();
            if (date != null) {
                item.setTime(date.toInstant());
            }
            item.setStyle(author.equals(own) || "me".equals(author) ? MessageListItemStyle.OWN : MessageListItemStyle.OTHER);
            if (support != null) {
                HorizontalLayout actions = HorizontalLayout.create(Space.SMALL);
                actions.add(createIconButton(PlatformIconGroup.actionsEdit(), TaskLocalize.issuesActionCommentEdit(), () ->
                    IssueCommentDialogDescriptor.showAsync(TaskLocalize.issuesActionCommentEdit(), comment.getText()).thenAccept(text -> {
                        if (text != null) {
                            run(support.editComment(task, comment, text));
                        }
                    })));
                actions.add(createIconButton(PlatformIconGroup.generalRemove(), TaskLocalize.issuesActionCommentDelete(), () ->
                    confirmAndRun(TaskLocalize.issuesConfirmDeleteComment(), () -> support.deleteComment(task, comment))));
                item.setActionsComponent(actions);
            }
        }

        DockLayout root = DockLayout.create(Space.NONE);
        root.center(messages);
        if (support != null) {
            MessageInput input = MessageInput.create();
            input.setPlaceholder(TaskLocalize.issuesCommentPlaceholder());
            input.setSendText(TaskLocalize.issuesActionCommentAdd());
            input.setSendIcon(PlatformIconGroup.actionsSend());
            input.addStyle(MessageInputStyle.INPLACE);
            input.addSubmitListener(event -> {
                String text = StringUtil.nullize(event.getValue(), true);
                if (text != null) {
                    run(support.addComment(task, text));
                }
            });
            DockLayout footer = DockLayout.create(Space.NONE);
            footer.center(input);
            footer.borderBuilder().topSet().apply();
            root.bottom(footer);
        }
        return root;
    }

    private @Nullable String getOwnName() {
        Account account = myRepository.getAccount();
        if (account == null) {
            return null;
        }
        String login = account.getLogin();
        return login != null ? login : account.getName();
    }

    @RequiredUIAccess
    private static Button createIconButton(Image icon, LocalizeValue tooltip, Runnable action) {
        Button button = Button.create(LocalizeValue.empty());
        button.setIcon(icon);
        button.addStyle(ButtonStyle.TOOLBAR);
        button.setToolTipText(tooltip);
        button.addClickListener(event -> action.run());
        return button;
    }

    @RequiredUIAccess
    private Component createWorkLogs(Task task, List<IssueWorkLog> workLogs) {
        ListBox<IssueWorkLog> list = ListBox.create(FlatDataModel.of(workLogs));
        list.setRenderer((presentation, item) -> {
            IssueWorkLog value = item.getValue();
            if (value != null) {
                presentation.withIcon(PlatformIconGroup.actionsLoginavatar());
                presentation.append(StringUtil.notNullize(value.getAuthor()), TextAttribute.REGULAR_BOLD);
                presentation.append("  " + value.getTimeSpent());
                presentation.append("  " + formatDate(value.getDate()), TextAttribute.GRAYED);
                if (!value.getComment().isEmpty()) {
                    presentation.append("  " + value.getComment());
                }
            }
        });
        IssueWorkLogSupport support = getTracker().getSupport(IssueWorkLogSupport.class);
        DefaultActionGroup group = new DefaultActionGroup();
        if (support != null) {
            group.add(new SimpleAction(TaskLocalize.issuesActionWorklogAdd(), PlatformIconGroup.generalAdd(), () -> true, () ->
                IssueWorkLogDialogDescriptor.showAsync(TaskLocalize.issuesActionWorklogAdd(), "", "").thenAccept(result -> {
                    if (result != null) {
                        run(support.addWorkLog(task, result.timeSpent(), result.comment()));
                    }
                })));
            group.add(new SimpleAction(TaskLocalize.issuesActionWorklogEdit(), PlatformIconGroup.actionsEdit(),
                () -> list.getValue() != null,
                () -> {
                    IssueWorkLog selected = list.getValue();
                    if (selected != null) {
                        IssueWorkLogDialogDescriptor.showAsync(TaskLocalize.issuesActionWorklogEdit(), selected.getTimeSpent(), selected.getComment())
                            .thenAccept(result -> {
                                if (result != null) {
                                    run(support.editWorkLog(task, selected, result.timeSpent(), result.comment()));
                                }
                            });
                    }
                }));
            group.add(new SimpleAction(TaskLocalize.issuesActionWorklogDelete(), PlatformIconGroup.generalRemove(),
                () -> list.getValue() != null,
                () -> {
                    IssueWorkLog selected = list.getValue();
                    if (selected != null) {
                        confirmAndRun(TaskLocalize.issuesConfirmDeleteWorklog(), () -> support.deleteWorkLog(task, selected));
                    }
                }));
        }
        return createToolWindowPanel(group, ScrollableLayout.create(list));
    }

    private static final class SimpleAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final Supplier<Boolean> myEnabled;
        private final Runnable myRunnable;

        private SimpleAction(LocalizeValue text, Image icon, Supplier<Boolean> enabled, Runnable runnable) {
            super(text, LocalizeValue.empty(), icon);
            myEnabled = enabled;
            myRunnable = runnable;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myRunnable.run();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myEnabled.get());
        }
    }

    private final class ChooseAction<T> extends DumbAwareAction implements DumbAware {
        private final LocalizeValue myTitle;
        private final Supplier<CompletableFuture<List<T>>> myValues;
        private final Function<T, String> myText;
        private final BiFunction<Task, T, CompletableFuture<?>> myApply;

        private ChooseAction(
            LocalizeValue title,
            Image icon,
            Supplier<CompletableFuture<List<T>>> values,
            Function<T, String> text,
            BiFunction<Task, T, CompletableFuture<?>> apply
        ) {
            super(title, LocalizeValue.empty(), icon);
            myTitle = title;
            myValues = values;
            myText = text;
            myApply = apply;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Task task = myTask;
            if (task == null) {
                return;
            }
            myValues.get().whenCompleteAsync((values, error) -> {
                if (values == null) {
                    if (error != null) {
                        MessageBoxes.okError(error.getCause() != null ? error.getCause() : error).showAsync();
                    }
                    return;
                }
                List<AnAction> actions = new ArrayList<>();
                for (T value : values) {
                    actions.add(new DumbAwareAction(LocalizeValue.of(myText.apply(value))) {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(AnActionEvent event) {
                            run(myApply.apply(task, value));
                        }
                    });
                }
                DefaultActionGroup group = new DefaultActionGroup();
                group.addAll(actions);
                ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    myTitle.get(),
                    group,
                    e.getDataContext(),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    false
                );
                popup.showUnderneathOf(e);
            }, UIAccess.current());
        }
    }
}
