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
package consulo.continuousIntegration.impl.internal.setting;

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
import consulo.continuousIntegration.CIManager;
import consulo.continuousIntegration.CIServerOptions;
import consulo.continuousIntegration.CIServerOptionsUtil;
import consulo.continuousIntegration.CIServerType;
import consulo.continuousIntegration.impl.internal.CIManagerImpl;
import consulo.continuousIntegration.impl.internal.CIServerEntry;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
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
 * Settings of the CI servers of a project: configured servers and the ones detected from accounts, grouped by type.
 * Removing a detected server hides it; changing one turns it into a configured server.
 *
 * @since 2026-11-09
 */
@ExtensionImpl
public class CIServersConfigurable extends UnifiedMasterDetailsComponent
    implements ProjectConfigurable, NonDefaultProjectConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    public static final String ID = "continuousIntegration.servers";

    private final Project myProject;
    private final AccountManager myAccountManager;
    private final List<CIServerDraft> myDrafts = new ArrayList<>();
    private final Set<String> myHiddenKeys = new LinkedHashSet<>();

    @Inject
    public CIServersConfigurable(Project project, AccountManager accountManager) {
        myProject = project;
        myAccountManager = accountManager;
    }

    private CIManagerImpl getManager() {
        return (CIManagerImpl) CIManager.getInstance(myProject);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ContinuousIntegrationImplLocalize.configurableServers();
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return ContinuousIntegrationImplLocalize.serversEmptySelection();
    }

    @Override
    protected boolean isExpandAllByDefault() {
        return true;
    }

    @Override
    protected boolean isRemovedWhenEmpty(Object value) {
        return value instanceof CIServerType;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof CIServerType<?> type) {
            return type.getPresentableName().get();
        }
        if (value instanceof CIServerDraft draft) {
            return getDraftText(draft);
        }
        return String.valueOf(value);
    }

    private String getDraftText(CIServerDraft draft) {
        CIServerOptions options = draft.getOptions();
        Account account = findAccount(options);
        String server = StringUtil.isEmpty(options.serverUrl) ? draft.getType().getPresentableName().get() : options.serverUrl;
        return account == null ? server : account.getName() + " @ " + server;
    }

    private @Nullable Account findAccount(CIServerOptions options) {
        String accountId = options.accountId;
        return accountId == null ? null : myAccountManager.findAccount(accountId, myProject);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof CIServerType<?> type) {
            presentation.withIcon(type.getIcon());
            presentation.append(type.getPresentableName(), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof CIServerDraft draft) {
            presentation.withIcon(draft.getType().getIcon());
            presentation.append(getDraftText(draft));
            if (findAccount(draft.getOptions()) == null) {
                presentation.append(LocalizeValue.space(), TextAttribute.GRAYED);
                LocalizeValue notLoggedIn = ContinuousIntegrationImplLocalize.serversNotLoggedIn(draft.getOptions().serverUrl);
                presentation.append(notLoggedIn, TextAttribute.GRAYED);
            }
            else if (!draft.isConfigured()) {
                presentation.append(LocalizeValue.space(), TextAttribute.GRAYED);
                presentation.append(ContinuousIntegrationImplLocalize.serversDetected(), TextAttribute.GRAYED);
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        myDrafts.clear();
        myHiddenKeys.clear();

        CIManagerImpl manager = getManager();
        myHiddenKeys.addAll(manager.getHiddenKeys());
        for (CIServerEntry entry : manager.getServerEntries()) {
            addDraftValue(CIServerDraft.of(entry));
        }
    }

    private void addDraftValue(CIServerDraft draft) {
        myDrafts.add(draft);
        if (!isAttached(draft.getType())) {
            addValue(null, draft.getType());
        }
        addValue(draft.getType(), draft);
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (value instanceof CIServerDraft draft) {
            return new CIServerDraftConfigurable(myProject, draft, this::getDraftText, () -> refreshValue(draft));
        }
        return null;
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddAction(), new RemoveAction());
    }

    private List<CIServerEntry> getConfiguredEntries() {
        List<CIServerEntry> entries = new ArrayList<>();
        for (CIServerDraft draft : myDrafts) {
            if (draft.isConfigured()) {
                entries.add(draft.toEntry());
            }
        }
        return entries;
    }

    /**
     * A detected server which was changed into another server is stored as configured; the detected one is hidden so
     * it does not come back next to it.
     */
    private Set<String> getEffectiveHiddenKeys() {
        Set<String> hidden = new LinkedHashSet<>(myHiddenKeys);
        for (CIServerDraft draft : myDrafts) {
            if (draft.isDetected() && draft.isChanged() && !draft.getCurrentKey().equals(draft.getKey())) {
                hidden.add(draft.getKey());
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

        CIManagerImpl manager = getManager();
        if (!manager.getHiddenKeys().equals(getEffectiveHiddenKeys())) {
            return true;
        }

        List<CIServerEntry> stored = new ArrayList<>();
        for (CIServerEntry entry : manager.getServerEntries()) {
            if (!entry.isDetected()) {
                stored.add(entry);
            }
        }
        List<CIServerEntry> current = getConfiguredEntries();
        if (stored.size() != current.size()) {
            return true;
        }
        for (int i = 0; i < stored.size(); i++) {
            CIServerEntry left = stored.get(i);
            CIServerEntry right = current.get(i);
            if (left.getType() != right.getType() || !CIServerOptionsUtil.isEqual(left.getOptions(), right.getOptions())) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        super.apply();
        getManager().setServerEntries(getConfiguredEntries(), getEffectiveHiddenKeys());
        reset();
    }

    @RequiredUIAccess
    private void addDraft(CIServerType<?> type) {
        CIServerOptions options = type.createOptions();
        AccountType<?> accountType = myAccountManager.findAccountType(type.getAccountTypeId());
        Account account = accountType == null ? null : myAccountManager.getActiveAccount(accountType, myProject);
        if (account != null) {
            options.accountId = account.getId();
            options.serverUrl = StringUtil.notNullize(myAccountManager.getServerUrl(account));
        }
        CIServerDraft draft = new CIServerDraft(type, options, null, false);
        addDraftValue(draft);
        refreshTree(draft);
    }

    private class AddAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private AddAction() {
            super(ContinuousIntegrationImplLocalize.serversActionAdd(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            myProject.getApplication().getExtensionPoint(CIServerType.class).forEach(type -> group.add(
                new DumbAwareAction(type.getPresentableName(), LocalizeValue.empty(), type.getIcon()) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        addDraft(type);
                    }
                }
            ));
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ContinuousIntegrationImplLocalize.serversActionAdd().get(),
                group.build(),
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myProject.getApplication().getExtensionPoint(CIServerType.class).hasAnyExtensions());
        }
    }

    private class RemoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveAction() {
            super(ContinuousIntegrationImplLocalize.serversActionRemove(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = getSelectedValues();
            if (!canRemove(selected)) {
                return;
            }
            for (Object value : selected) {
                CIServerDraft draft = (CIServerDraft) value;
                myDrafts.remove(draft);
                if (draft.isDetected()) {
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
                if (!(value instanceof CIServerDraft)) {
                    return false;
                }
            }
            return true;
        }
    }
}
