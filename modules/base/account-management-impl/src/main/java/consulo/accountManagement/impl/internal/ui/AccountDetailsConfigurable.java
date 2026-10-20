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
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountCredentialSource;
import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountFeatureAction;
import consulo.accountManagement.AccountFeatureState;
import consulo.accountManagement.AccountLevel;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountScope;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.impl.internal.AccountManagerImpl;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AvatarStyle;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
class AccountDetailsConfigurable implements MasterDetailsConfigurable<Account> {
    private final AccountManager myManager;
    private final AccountComponentFactory myComponentFactory;
    private final Account myAccount;

    private @Nullable VerticalLayout myRoot;
    private @Nullable Disposable myContentDisposable;

    AccountDetailsConfigurable(AccountManager manager, AccountComponentFactory componentFactory, Account account) {
        myManager = manager;
        myComponentFactory = componentFactory;
        myAccount = account;
    }

    @Override
    public @Nullable Image getIcon() {
        AccountType<?> type = myManager.findAccountType(myAccount.getTypeId());
        return type == null ? null : type.getIcon();
    }

    @Override
    public Account getEditableObject() {
        return myAccount;
    }

    @Override
    public void updateName() {
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return LocalizeValue.of(myAccount.getName());
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myAccount.getName());
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        VerticalLayout root = VerticalLayout.create(Space.X_LARGE);
        myRoot = root;
        fill(root, parentDisposable);

        UIAccess uiAccess = UIAccess.current();
        myManager.addListener(new AccountListener() {
            @Override
            public void accountChanged(Account account) {
                if (account == myAccount) {
                    uiAccess.execute(() -> fill(root, parentDisposable));
                }
            }

            @Override
            public void activeAccountChanged(AccountType<?> type, @Nullable Account account) {
                uiAccess.execute(() -> fill(root, parentDisposable));
            }
        }, parentDisposable);
        return root;
    }

    @RequiredUIAccess
    private void fill(VerticalLayout root, Disposable parentDisposable) {
        Disposable previous = myContentDisposable;
        if (previous != null) {
            Disposer.dispose(previous);
        }
        Disposable contentDisposable = Disposable.newDisposable();
        Disposer.register(parentDisposable, contentDisposable);
        myContentDisposable = contentDisposable;

        root.removeAll();

        AccountType<?> type = myManager.findAccountType(myAccount.getTypeId());

        HorizontalLayout header = HorizontalLayout.create(Space.LARGE);
        header.add(myComponentFactory.createAvatar(myAccount, AvatarStyle.LARGE, contentDisposable));
        VerticalLayout title = VerticalLayout.create(Space.NONE);
        Label name = Label.create(LocalizeValue.of(myAccount.getName()));
        name.addStyle(LabelStyle.FONT_LARGE);
        name.addStyle(LabelStyle.FONT_BOLD);
        title.add(name);
        if (type != null) {
            Label typeLabel = Label.create(type.getName());
            typeLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            title.add(typeLabel);
        }
        header.add(title);
        VerticalLayout reloginColumn = VerticalLayout.create(Space.NONE);
        reloginColumn.add(Button.create(AccountManagementLocalize.actionRelogin(), event -> myManager.requestAuthorization(myAccount, null, null, event.getComponent())));
        root.add(DockLayout.create().left(header).right(reloginColumn));

        FormBuilder form = FormBuilder.create();
        String server = myAccount.getServerUrl();
        if (server != null) {
            form.addLabeled(AccountManagementLocalize.labelServer(), Label.create(LocalizeValue.of(server)));
        }
        String login = myAccount.getLogin();
        if (login != null) {
            form.addLabeled(AccountManagementLocalize.labelLogin(), Label.create(LocalizeValue.of(login)));
        }
        AccountAuthorizeType<?> authorizeType = myManager.findAuthorizeType(myAccount.getAuthorizeTypeId());
        form.addLabeled(
            AccountManagementLocalize.labelAuthorizeType(),
            Label.create(authorizeType == null ? LocalizeValue.of(myAccount.getAuthorizeTypeId()) : authorizeType.getName())
        );
        AccountCredentialSource source = myManager instanceof AccountManagerImpl impl ? impl.findCredentialSource(myAccount.getCredentialSourceId()) : null;
        form.addLabeled(
            AccountManagementLocalize.labelCredentialSource(),
            Label.create(source == null ? LocalizeValue.of(myAccount.getCredentialSourceId()) : source.getName())
        );
        form.addLabeled(
            AccountManagementLocalize.labelLevel(),
            Label.create(myAccount.getLevel() == AccountLevel.PROJECT ? AccountManagementLocalize.levelProject() : AccountManagementLocalize.levelApplication())
        );
        root.add(form.build());

        if (type != null) {
            addFeatures(root, type);
        }
    }

    @RequiredUIAccess
    private <S extends AccountScope> void addFeatures(VerticalLayout root, AccountType<S> type) {
        List<AccountFeature<S>> features = myManager.getFeatures(type);
        if (features.isEmpty()) {
            return;
        }
        Label header = Label.create(AccountManagementLocalize.labelAvailableIntegrations());
        header.addStyle(LabelStyle.FONT_BOLD);
        root.add(header);

        for (AccountFeature<S> feature : features) {
            VerticalLayout text = VerticalLayout.create(Space.X_SMALL);

            HorizontalLayout titleRow = HorizontalLayout.create(Space.MEDIUM);
            Label title = Label.create(feature.getTitle());
            title.addStyle(LabelStyle.FONT_BOLD);
            titleRow.add(title);
            String infoUrl = feature.getInfoUrl();
            if (infoUrl != null) {
                Hyperlink info = Hyperlink.create(AccountManagementLocalize.actionMoreInfo(), event -> BrowserUtil.browse(infoUrl));
                info.setIcon(PlatformIconGroup.ideExternallink());
                titleRow.add(info);
            }
            text.add(titleRow);

            if (feature.getDescription() != LocalizeValue.empty()) {
                Label description = Label.create(feature.getDescription());
                description.setForegroundColor(ComponentColors.DISABLED_TEXT);
                text.add(description);
            }
            Set<S> scopes = feature.getRequiredScopes();
            if (!scopes.isEmpty()) {
                List<String> ids = new ArrayList<>();
                for (S scope : scopes) {
                    ids.add(scope.getId());
                }
                Label scopeLabel = Label.create(AccountManagementLocalize.labelScopes(String.join(", ", ids)));
                scopeLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
                scopeLabel.addStyle(LabelStyle.FONT_SMALL);
                text.add(scopeLabel);
            }
            for (AccountFeatureAction action : feature.getActions()) {
                Hyperlink link = Hyperlink.create(action.getText(), event -> action.perform(myAccount, null, event.getComponent()));
                if (action.isExternal()) {
                    link.setIcon(PlatformIconGroup.ideExternallink());
                }
                text.add(link);
            }

            AccountFeatureState state = myManager.getState(feature, myAccount);
            Button stateButton;
            if (state == AccountFeatureState.MISSING_SCOPES || state == AccountFeatureState.AUTH_FAILED) {
                stateButton = Button.create(AccountManagementLocalize.actionAllow(), event -> myManager.requestAuthorization(myAccount, feature, null, event.getComponent()));
                stateButton.setToolTipText(stateText(state, myManager.getMissingScopes(feature, myAccount)));
            }
            else {
                stateButton = Button.create(state == AccountFeatureState.UNKNOWN ? AccountManagementLocalize.stateUnknown() : AccountManagementLocalize.stateAllowed());
                stateButton.setEnabled(false);
            }

            VerticalLayout buttonColumn = VerticalLayout.create(Space.NONE);
            buttonColumn.add(stateButton);
            root.add(DockLayout.create().left(text).right(buttonColumn));
        }
    }

    private static <S extends AccountScope> LocalizeValue stateText(AccountFeatureState state, Set<S> missing) {
        return switch (state) {
            case AUTHORIZED -> AccountManagementLocalize.stateAuthorized();
            case AUTH_FAILED -> AccountManagementLocalize.stateAuthFailed();
            case UNKNOWN -> AccountManagementLocalize.stateUnknown();
            case MISSING_SCOPES -> {
                List<String> ids = new ArrayList<>();
                for (S scope : missing) {
                    ids.add(scope.getId());
                }
                yield AccountManagementLocalize.stateMissingScopes(StringUtil.join(ids, ", "));
            }
        };
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
}
