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
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.accountManagement.ui.AccountChooser;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
class AccountChooserImpl implements AccountChooser {
    private final AccountManager myManager;
    private final AccountType<?> myType;
    private final @Nullable Project myProject;
    private final MutableFlatDataModel<Account> myModel;
    private final ComboBox<Account> myComboBox;
    private final DockLayout myComponent;
    private final List<Consumer<@Nullable Account>> myListeners = new CopyOnWriteArrayList<>();

    @RequiredUIAccess
    AccountChooserImpl(AccountManager manager, AccountType<?> type, @Nullable Project project, Disposable parentDisposable) {
        myManager = manager;
        myType = type;
        myProject = project;
        myModel = FlatDataModel.of(manager.getAccounts(type, project));
        myComboBox = ComboBox.create(myModel);
        myComboBox.setRenderer((presentation, item) -> {
            Account account = item.getValue();
            if (account == null) {
                presentation.append(AccountManagementLocalize.labelNoAccount(), TextAttribute.GRAYED);
                return;
            }
            Image avatar = myManager.getAvatar(account).get();
            presentation.withIcon(avatar != null ? avatar : myType.getIcon());
            presentation.append(LocalizeValue.of(account.getName()));
            if (account.isAuthFailed()) {
                presentation.append("  " + AccountManagementLocalize.labelReloginNeeded().get(), TextAttribute.ERROR);
            }
        });
        myComboBox.setValue(manager.getActiveAccount(type, project));
        myComboBox.addValueListener(event -> fire(event.getValue()));

        Hyperlink add = Hyperlink.create(AccountManagementLocalize.actionAddAccount(), event ->
            myManager.requestLogin(myType, null, myProject, event.getComponent()).whenCompleteAsync((account, error) -> {
                if (account != null) {
                    setValue(account);
                }
            }, UIAccess.current())
        );
        myComponent = DockLayout.create().center(myComboBox).right(add);

        UIAccess uiAccess = UIAccess.current();
        manager.addListener(new AccountListener() {
            @Override
            public void accountsChanged() {
                uiAccess.execute(() -> reload());
            }
        }, parentDisposable);
    }

    @RequiredUIAccess
    private void reload() {
        Account selected = myComboBox.getValue();
        myModel.replaceAll(myManager.getAccounts(myType, myProject));
        if (selected != null && myModel.indexOf(selected) >= 0) {
            myComboBox.setValue(selected);
        }
        else {
            myComboBox.setValue(myManager.getActiveAccount(myType, myProject));
        }
    }

    private void fire(@Nullable Account account) {
        for (Consumer<@Nullable Account> listener : myListeners) {
            listener.accept(account);
        }
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    @Override
    public @Nullable Account getValue() {
        return myComboBox.getValue();
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable Account account) {
        myComboBox.setValue(account);
    }

    @Override
    public Runnable addValueListener(Consumer<@Nullable Account> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }
}
