package consulo.task.impl.internal.setting;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.TaskRepositoryOptions;
import consulo.task.TaskRepositoryOptionsUtil;
import consulo.task.TaskRepositoryType;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.impl.internal.TaskRepositoryEntry;
import consulo.task.localize.TaskLocalize;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskRepositoriesConfigurable extends UnifiedMasterDetailsComponent
    implements ProjectConfigurable, NonDefaultProjectConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    private final Project myProject;
    private final AccountManager myAccountManager;
    private final List<TaskRepositoryDraft> myDrafts = new ArrayList<>();
    private final Set<String> myHiddenKeys = new LinkedHashSet<>();

    @Inject
    public TaskRepositoriesConfigurable(Project project, AccountManager accountManager) {
        myProject = project;
        myAccountManager = accountManager;
    }

    private TaskManagerImpl getManager() {
        return (TaskManagerImpl) TaskManager.getManager(myProject);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return TaskLocalize.serversConfigurable();
    }

    @Override
    public String getId() {
        return "tasks.servers";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.TASKS_GROUP;
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return TaskLocalize.serversEmptySelection();
    }

    @Override
    protected boolean isExpandAllByDefault() {
        return true;
    }

    @Override
    protected boolean isRemovedWhenEmpty(Object value) {
        return value instanceof TaskRepositoryType;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof TaskRepositoryType<?> type) {
            return type.getPresentableName().get();
        }
        if (value instanceof TaskRepositoryDraft draft) {
            return getDraftText(draft);
        }
        return String.valueOf(value);
    }

    private String getDraftText(TaskRepositoryDraft draft) {
        String accountId = draft.options.accountId;
        Account account = accountId == null ? null : myAccountManager.findAccount(accountId, myProject);
        String server = StringUtil.isEmpty(draft.options.serverUrl) ? draft.type.getPresentableName().get() : draft.options.serverUrl;
        return account == null ? server : account.getName() + " @ " + server;
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof TaskRepositoryType<?> type) {
            presentation.withIcon(type.getIcon());
            presentation.append(type.getPresentableName(), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof TaskRepositoryDraft draft) {
            presentation.withIcon(draft.type.getIcon());
            presentation.append(getDraftText(draft));
            if (draft.options.accountId == null || myAccountManager.findAccount(draft.options.accountId, myProject) == null) {
                String server = draft.options.serverUrl.isEmpty() ? draft.type.getPresentableName().get() : draft.options.serverUrl;
                presentation.append(" (" + TaskLocalize.serversNotLoggedIn(server).get() + ")", TextAttribute.GRAYED);
            }
            else if (!draft.isConfigured()) {
                presentation.append(" (" + TaskLocalize.serversDetected().get() + ")", TextAttribute.GRAYED);
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        myDrafts.clear();
        myHiddenKeys.clear();

        TaskManagerImpl manager = getManager();
        myHiddenKeys.addAll(manager.getHiddenKeys());
        for (TaskRepositoryEntry entry : manager.getRepositoryEntries()) {
            addDraftValue(TaskRepositoryDraft.of(entry));
        }
    }

    private void addDraftValue(TaskRepositoryDraft draft) {
        myDrafts.add(draft);
        if (!isAttached(draft.type)) {
            addValue(null, draft.type);
        }
        addValue(draft.type, draft);
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (value instanceof TaskRepositoryDraft draft) {
            return new TaskRepositoryDraftConfigurable(myProject, draft, this::getDraftText, () -> refreshValue(draft));
        }
        return null;
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddAction(), new RemoveAction());
    }

    private List<TaskRepositoryEntry> getConfiguredEntries() {
        List<TaskRepositoryEntry> entries = new ArrayList<>();
        for (TaskRepositoryDraft draft : myDrafts) {
            if (draft.isConfigured()) {
                entries.add(draft.toEntry());
            }
        }
        return entries;
    }

    private Set<String> getEffectiveHiddenKeys() {
        Set<String> hidden = new LinkedHashSet<>(myHiddenKeys);
        for (TaskRepositoryDraft draft : myDrafts) {
            if (draft.detected && draft.isChanged()) {
                String key = draft.type.getDetectionKeyUnchecked(draft.options);
                if (!key.equals(draft.getKey())) {
                    hidden.add(draft.getKey());
                }
            }
        }
        return hidden;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (super.isModified()) {
            return true;
        }
        TaskManagerImpl manager = getManager();
        if (!manager.getHiddenKeys().equals(getEffectiveHiddenKeys())) {
            return true;
        }
        List<TaskRepositoryEntry> stored = new ArrayList<>();
        for (TaskRepositoryEntry entry : manager.getRepositoryEntries()) {
            if (!entry.isDetected()) {
                stored.add(entry);
            }
        }
        List<TaskRepositoryEntry> current = getConfiguredEntries();
        if (stored.size() != current.size()) {
            return true;
        }
        for (int i = 0; i < stored.size(); i++) {
            TaskRepositoryEntry left = stored.get(i);
            TaskRepositoryEntry right = current.get(i);
            if (left.getType() != right.getType() || !TaskRepositoryOptionsUtil.isEqual(left.getOptions(), right.getOptions())) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        super.apply();
        TaskManagerImpl manager = getManager();
        manager.setRepositoryEntries(getConfiguredEntries(), getEffectiveHiddenKeys());
        manager.updateIssues(null);
        reset();
    }

    @RequiredUIAccess
    private void addDraft(TaskRepositoryType<?> type) {
        TaskRepositoryOptions options = type.createOptions();
        AccountType<?> accountType = myAccountManager.findAccountType(type.getAccountTypeId());
        Account account = accountType == null ? null : myAccountManager.getActiveAccount(accountType, myProject);
        if (account != null) {
            options.accountId = account.getId();
            options.serverUrl = StringUtil.notNullize(myAccountManager.getServerUrl(account));
        }
        TaskRepositoryDraft draft = new TaskRepositoryDraft(type, options, null, false);
        addDraftValue(draft);
        refreshTree(draft);
    }

    private class AddAction extends DumbAwareAction {
        private AddAction() {
            super(TaskLocalize.serversActionAdd(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            DefaultActionGroup group = new DefaultActionGroup();
            myProject.getApplication().getExtensionPoint(TaskRepositoryType.class).forEach(type -> group.add(
                new DumbAwareAction(type.getPresentableName(), LocalizeValue.empty(), type.getIcon()) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        addDraft(type);
                    }
                }
            ));
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                TaskLocalize.serversActionAdd().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class RemoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveAction() {
            super(TaskLocalize.serversActionRemove(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = getSelectedValues();
            if (!canRemove(selected)) {
                return;
            }
            for (Object value : selected) {
                TaskRepositoryDraft draft = (TaskRepositoryDraft) value;
                myDrafts.remove(draft);
                if (draft.detected) {
                    myHiddenKeys.add(draft.getKey());
                }
            }
            removeSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(canRemove(getSelectedValues()));
        }

        private boolean canRemove(List<Object> selected) {
            if (selected.isEmpty()) {
                return false;
            }
            for (Object value : selected) {
                if (!(value instanceof TaskRepositoryDraft)) {
                    return false;
                }
            }
            return true;
        }
    }
}
