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
package consulo.accountManagement.impl.internal.ui;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class AccountConfigurable implements ApplicationConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    private final AccountManager myManager;
    private final AccountComponentFactory myComponentFactory;
    private final List<Runnable> mySubscriptions = new ArrayList<>();

    private @Nullable ListBox<Object> myList;
    private @Nullable MutableFlatDataModel<Object> myModel;
    private @Nullable DockLayout myDetailsPanel;
    private @Nullable Disposable myDetailsDisposable;
    private @Nullable Disposable myUIDisposable;

    @Inject
    public AccountConfigurable(AccountManager manager, AccountComponentFactory componentFactory) {
        myManager = manager;
        myComponentFactory = componentFactory;
    }

    @Override
    public String getId() {
        return StandardConfigurableIds.ACCOUNTS;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.ACCOUNTS_AND_PASSWORDS_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AccountManagementLocalize.configurableDisplayName();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        myUIDisposable = parentDisposable;

        MutableFlatDataModel<Object> model = FlatDataModel.of(List.of());
        myModel = model;

        ListBox<Object> list = ListBox.create(model);
        list.isSeparator(value -> value instanceof AccountType);
        list.setSeparatorCaption(value -> value instanceof AccountType<?> type ? type.getName() : LocalizeValue.empty());
        list.setRenderer(ComponentItemRenderer.reusable(() -> new AccountRow(myManager).getComponent(), (component, item) -> {
            AccountRow row = component.getUserData(AccountRow.KEY);
            if (row != null) {
                row.bind(item);
            }
        }));
        list.addValueListener(event -> showDetails(event.getValue()));
        myList = list;

        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new AddAction())
            .add(new SetActiveAction())
            .add(new LogOutAction())
            .build();
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(list);

        DockLayout master = DockLayout.create(Space.NONE);
        master.top(toolbar.getUIComponent());
        master.center(ScrollableLayout.create(list));

        DockLayout details = DockLayout.create(Space.NONE);
        details.paddingBuilder().allSet(Space.X_LARGE).apply();
        myDetailsPanel = details;

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(master);
        split.setSecondComponent(details);
        split.setProportion(30);

        UIAccess uiAccess = UIAccess.current();
        myManager.addListener(new AccountListener() {
            @Override
            public void accountsChanged() {
                uiAccess.execute(() -> reload());
            }

            @Override
            public void accountChanged(Account account) {
                uiAccess.execute(() -> model.update(account));
            }

            @Override
            public void activeAccountChanged(AccountType<?> type, @Nullable Account account) {
                uiAccess.execute(() -> reload());
            }
        }, parentDisposable);

        reload();
        return split;
    }

    @RequiredUIAccess
    private void reload() {
        MutableFlatDataModel<Object> model = myModel;
        ListBox<Object> list = myList;
        if (model == null || list == null) {
            return;
        }
        unsubscribe();

        Map<AccountType<?>, List<Account>> grouped = new LinkedHashMap<>();
        List<AccountType<?>> types = new ArrayList<>(myManager.getAccountTypes());
        types.sort(Comparator.comparing(type -> type.getName().get(), String.CASE_INSENSITIVE_ORDER));
        for (AccountType<?> type : types) {
            List<Account> accounts = myManager.getAccounts(type, null);
            if (!accounts.isEmpty()) {
                grouped.put(type, accounts);
            }
        }

        UIAccess uiAccess = UIAccess.current();
        List<Object> items = new ArrayList<>();
        for (Map.Entry<AccountType<?>, List<Account>> entry : grouped.entrySet()) {
            items.add(entry.getKey());
            for (Account account : entry.getValue()) {
                items.add(account);
                mySubscriptions.add(myManager.getAvatar(account).addListener(image -> uiAccess.execute(() -> model.update(account))));
            }
        }

        Object selected = list.getValue();
        model.replaceAll(items);
        Object toSelect = selected != null && items.contains(selected) ? selected : firstAccount(items);
        list.setValue(toSelect);
        showDetails(toSelect);
    }

    private static @Nullable Object firstAccount(List<Object> items) {
        for (Object item : items) {
            if (item instanceof Account) {
                return item;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void showDetails(@Nullable Object value) {
        DockLayout panel = myDetailsPanel;
        Disposable uiDisposable = myUIDisposable;
        if (panel == null || uiDisposable == null) {
            return;
        }
        Disposable previous = myDetailsDisposable;
        if (previous != null) {
            Disposer.dispose(previous);
        }
        if (!(value instanceof Account account)) {
            myDetailsDisposable = null;
            Label empty = Label.create(AccountManagementLocalize.labelEmptySelection());
            panel.center(DockLayout.create().center(empty));
            return;
        }
        Disposable disposable = Disposable.newDisposable();
        Disposer.register(uiDisposable, disposable);
        myDetailsDisposable = disposable;
        panel.center(new AccountDetailsConfigurable(myManager, myComponentFactory, account).createUIComponent(disposable));
    }

    private void unsubscribe() {
        for (Runnable subscription : mySubscriptions) {
            subscription.run();
        }
        mySubscriptions.clear();
    }

    private @Nullable Account getSelectedAccount() {
        ListBox<Object> list = myList;
        return list != null && list.getValue() instanceof Account account ? account : null;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myList;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        unsubscribe();
        myList = null;
        myModel = null;
        myDetailsPanel = null;
        myDetailsDisposable = null;
        myUIDisposable = null;
    }

    private class AddAction extends DumbAwareAction {
        private AddAction() {
            super(AccountManagementLocalize.actionAddAccount(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            DefaultActionGroup group = new DefaultActionGroup();
            for (AccountType<?> type : myManager.getAccountTypes()) {
                List<? extends AccountAuthorizeConfig> configs = myManager.getAuthorizeConfigs(type);
                if (configs.isEmpty()) {
                    continue;
                }
                if (configs.size() == 1) {
                    group.add(new LoginAction(type, configs.getFirst(), type.getName(), type.getIcon()));
                    continue;
                }
                DefaultActionGroup sub = new DefaultActionGroup(type.getName(), true);
                sub.getTemplatePresentation().setIcon(type.getIcon());
                for (AccountAuthorizeConfig config : configs) {
                    AccountAuthorizeType<?> authorizeType = myManager.findAuthorizeType(config);
                    if (authorizeType != null) {
                        sub.add(new LoginAction(type, config, authorizeType.getName(), null));
                    }
                }
                group.add(sub);
            }

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                AccountManagementLocalize.actionAddAccount().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class LoginAction extends DumbAwareAction {
        private final AccountType<?> myType;
        private final AccountAuthorizeConfig myConfig;

        private LoginAction(AccountType<?> type, AccountAuthorizeConfig config, LocalizeValue text, @Nullable Image icon) {
            super(text, LocalizeValue.empty(), icon);
            myType = type;
            myConfig = config;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myManager.requestLogin(myType, myConfig, null, myList).whenCompleteAsync((account, error) -> {
                ListBox<Object> list = myList;
                if (account != null && list != null) {
                    list.setValue(account);
                    showDetails(account);
                }
            }, UIAccess.current());
        }
    }

    private class SetActiveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private SetActiveAction() {
            super(AccountManagementLocalize.actionSetActive(), LocalizeValue.empty(), PlatformIconGroup.actionsSwappanels());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Account account = getSelectedAccount();
            AccountType<?> type = account == null ? null : myManager.findAccountType(account.getTypeId());
            if (account != null && type != null) {
                myManager.setActiveAccount(type, account, null);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            Account account = getSelectedAccount();
            AccountType<?> type = account == null ? null : myManager.findAccountType(account.getTypeId());
            e.getPresentation().setEnabled(type != null && myManager.getActiveAccount(type, null) != account);
        }
    }

    private class LogOutAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private LogOutAction() {
            super(AccountManagementLocalize.actionLogOut(), LocalizeValue.empty(), PlatformIconGroup.actionsExit());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Account account = getSelectedAccount();
            if (account != null) {
                myManager.removeAccount(account, null);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedAccount() != null);
        }
    }
}
