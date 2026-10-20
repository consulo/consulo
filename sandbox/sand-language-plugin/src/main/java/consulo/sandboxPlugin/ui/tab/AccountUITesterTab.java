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
package consulo.sandboxPlugin.ui.tab;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountRequestDecorator;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.ui.AccountChooser;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.ide.account.SandHubAccountType;
import consulo.sandboxPlugin.ide.account.SandHubScope;
import consulo.ui.AvatarStyle;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl(id = "account", order = "after badge")
public class AccountUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Accounts");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Application application = Application.get();
        AccountManager manager = application.getInstance(AccountManager.class);
        AccountComponentFactory factory = application.getInstance(AccountComponentFactory.class);
        AccountType<?> type = manager.findAccountType(SandHubAccountType.ID);

        VerticalLayout root = VerticalLayout.create(Space.LARGE);
        if (type == null) {
            root.add(Label.create(LocalizeValue.localizeTODO("Sand Hub account type is not registered")));
            return root;
        }

        AccountChooser chooser = factory.createChooser(type, null, uiDisposable);
        root.add(chooser.getComponent());
        UIAccess uiAccess = UIAccess.current();
        Label connection = Label.create(LocalizeValue.localizeTODO("Not connected"));
        Button connect = Button.create(LocalizeValue.localizeTODO("Connect (decorate a request)"), event -> {
            Account account = chooser.getValue();
            if (account == null) {
                connection.setText(LocalizeValue.localizeTODO("No account selected"));
                return;
            }
            connection.setText(LocalizeValue.localizeTODO("Connecting…"));
            manager.connect(account).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    connection.setText(LocalizeValue.of("Failed: " + error.getMessage()));
                    return;
                }
                StringBuilder builder = new StringBuilder("Server " + result.getServerUrl() + ":");
                result.decorate(new AccountRequestDecorator() {
                    @Override
                    public void header(String name, String value) {
                        builder.append(" header ").append(name).append('=').append(value);
                    }

                    @Override
                    public void queryParameter(String name, String value) {
                        builder.append(" param ").append(name).append('=').append(value);
                    }
                });
                connection.setText(LocalizeValue.of(builder.toString()));
            }, uiAccess);
        });
        HorizontalLayout connectRow = HorizontalLayout.create(Space.MEDIUM);
        connectRow.add(connect);
        connectRow.add(connection);
        root.add(connectRow);

        HorizontalLayout details = HorizontalLayout.create(Space.MEDIUM);
        root.add(details);
        Label states = Label.create();
        root.add(states);

        Runnable update = () -> {
            details.removeAll();
            Account account = chooser.getValue();
            if (account == null) {
                states.setText(LocalizeValue.localizeTODO("No account selected"));
                return;
            }
            details.add(factory.createAvatar(account, AvatarStyle.SMALL, uiDisposable));
            details.add(factory.createAvatar(account, AvatarStyle.LARGE, uiDisposable));
            details.add(Label.create(LocalizeValue.of(account.getName() + " - scopes " + account.getGrantedScopeIds())));
            states.setText(LocalizeValue.of(describeStates(manager, type, account)));
        };
        chooser.addValueListener(account -> uiAccess.execute(update));
        update.run();

        return root;
    }

    private static String describeStates(AccountManager manager, AccountType<?> type, Account account) {
        StringBuilder builder = new StringBuilder();
        for (AccountFeature<SandHubScope> feature : manager.getFeatures(sandHub(type))) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(feature.getTitle().get()).append(": ").append(manager.getState(feature, account));
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static AccountType<SandHubScope> sandHub(AccountType<?> type) {
        return (AccountType<SandHubScope>) type;
    }
}
