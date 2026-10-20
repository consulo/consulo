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
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountScope;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.impl.internal.AccountLoginResult;
import consulo.accountManagement.impl.internal.AccountManagerImpl;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class AccountLoginDialogDescriptor extends DialogDescriptor {
    private final AccountManagerImpl myManager;
    private final AccountType<?> myType;
    private final AccountAuthorizeConfig myConfig;
    private final AccountAuthorizeType<?> myAuthorizeType;
    private final @Nullable Account myAccount;
    private final @Nullable AccountFeature<?> myFeature;
    private final @Nullable Project myProject;

    private final Map<AccountFeature<?>, CheckBox> myFeatureBoxes = new LinkedHashMap<>();
    private @Nullable TextBox myServerBox;
    private @Nullable Label myScopesLabel;
    private @Nullable Label myErrorLabel;
    private @Nullable AccountLoginSession<?> mySession;
    private @Nullable Dialog myDialog;
    private @Nullable AccountLoginResult myResult;
    private boolean myBusy;

    private AccountLoginDialogDescriptor(
        AccountManagerImpl manager,
        AccountType<?> type,
        AccountAuthorizeConfig config,
        AccountAuthorizeType<?> authorizeType,
        @Nullable Account account,
        @Nullable AccountFeature<?> feature,
        @Nullable Project project
    ) {
        super(account == null ? AccountManagementLocalize.dialogLoginTitle(type.getName()) : AccountManagementLocalize.dialogAuthorizeTitle(account.getName()));
        myManager = manager;
        myType = type;
        myConfig = config;
        myAuthorizeType = authorizeType;
        myAccount = account;
        myFeature = feature;
        myProject = project;
    }

    @RequiredUIAccess
    public static CompletableFuture<@Nullable AccountLoginResult> show(
        AccountManagerImpl manager,
        AccountType<?> type,
        AccountAuthorizeConfig config,
        @Nullable Account account,
        @Nullable AccountFeature<?> feature,
        @Nullable Project project,
        @Nullable Component parent
    ) {
        AccountAuthorizeType<?> authorizeType = manager.findAuthorizeType(config);
        if (authorizeType == null) {
            return CompletableFuture.completedFuture(null);
        }
        AccountLoginDialogDescriptor descriptor = new AccountLoginDialogDescriptor(manager, type, config, authorizeType, account, feature, project);
        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Dialog dialog = parent == null ? dialogService.build(descriptor) : dialogService.build(parent, descriptor);
        descriptor.myDialog = dialog;
        return dialog.showAsync().handleAsync((value, error) -> error != null || value == null ? null : descriptor.myResult, UIAccess.current());
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(AccountManagementLocalize.buttonLogIn());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create(Space.MEDIUM);

        if (myType.isServerEditable()) {
            String server = myAccount != null ? myAccount.getServerUrl() : myType.getDefaultServerUrl();
            TextBox serverBox = TextBox.create(StringUtil.notNullize(server));
            serverBox.setEnabled(myAccount == null);
            myServerBox = serverBox;
            root.add(FormBuilder.create().addLabeled(AccountManagementLocalize.labelServer(), serverBox).build());
        }

        List<AccountFeature<?>> features = new ArrayList<>(myManager.getFeatures(myType));
        if (!features.isEmpty()) {
            Label header = Label.create(AccountManagementLocalize.labelFeatures());
            header.addStyle(LabelStyle.FONT_BOLD);
            root.add(header);
            for (AccountFeature<?> feature : features) {
                root.add(createFeatureRow(feature));
            }
        }
        if (!features.isEmpty() && !myType.getAllScopes().isEmpty()) {
            Label scopesLabel = Label.create();
            scopesLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            myScopesLabel = scopesLabel;
            root.add(scopesLabel);
        }

        AccountLoginSession<?> session = AccountLoginSession.create(myAuthorizeType, new Context(uiDisposable));
        mySession = session;
        Component formComponent = session.getForm().getComponent();
        if (formComponent != null) {
            root.add(formComponent);
        }
        else {
            root.add(Label.create(AccountManagementLocalize.labelNoCredentialsNeeded()));
        }

        Label errorLabel = Label.create();
        errorLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        errorLabel.setVisible(false);
        myErrorLabel = errorLabel;
        root.add(errorLabel);

        updateScopes();
        return root;
    }

    @RequiredUIAccess
    private Component createFeatureRow(AccountFeature<?> feature) {
        boolean locked = !feature.isOptional() || feature == myFeature;
        CheckBox box = CheckBox.create(feature.getTitle(), true);
        box.setEnabled(!locked);
        box.addValueListener(event -> updateScopes());
        myFeatureBoxes.put(feature, box);

        VerticalLayout row = VerticalLayout.create(Space.NONE);
        row.add(box);
        LocalizeValue description = feature.getDescription();
        if (description != LocalizeValue.empty()) {
            Label descriptionLabel = Label.create(description);
            descriptionLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            descriptionLabel.addStyle(LabelStyle.FONT_SMALL);
            row.add(descriptionLabel);
        }
        return row;
    }

    private Set<AccountScope> getScopes() {
        Set<AccountScope> scopes = new LinkedHashSet<>(myType.getBaseScopes());
        for (Map.Entry<AccountFeature<?>, CheckBox> entry : myFeatureBoxes.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue().getValue())) {
                scopes.addAll(entry.getKey().getRequiredScopes());
                scopes.addAll(entry.getKey().getOptionalScopes());
            }
        }
        if (myAccount != null) {
            Set<String> granted = myAccount.getGrantedScopeIds();
            for (AccountScope scope : myType.getAllScopes()) {
                if (granted.contains(scope.getId())) {
                    scopes.add(scope);
                }
            }
        }
        return scopes;
    }

    private @Nullable String getServerUrl() {
        TextBox serverBox = myServerBox;
        if (serverBox != null) {
            return StringUtil.nullize(StringUtil.notNullize(serverBox.getValue()).trim());
        }
        return myAccount != null ? myAccount.getServerUrl() : myType.getDefaultServerUrl();
    }

    @RequiredUIAccess
    private void updateScopes() {
        Label scopesLabel = myScopesLabel;
        if (scopesLabel == null) {
            return;
        }
        List<String> ids = new ArrayList<>();
        for (AccountScope scope : getScopes()) {
            ids.add(scope.getId());
        }
        scopesLabel.setText(AccountManagementLocalize.labelRequestedScopes(ids.isEmpty() ? "-" : String.join(", ", ids)));
    }

    @RequiredUIAccess
    private void showError(LocalizeValue message) {
        Label errorLabel = myErrorLabel;
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(message != LocalizeValue.empty());
        }
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        AccountLoginSession<?> session = mySession;
        return session == null ? null : session.getForm().getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (!(action instanceof DialogOkAction) || myResult != null) {
            return true;
        }
        AccountLoginSession<?> session = mySession;
        if (myBusy || session == null) {
            return false;
        }
        LocalizeValue error = session.getForm().validate();
        if (error == LocalizeValue.empty() && myType.isServerEditable() && getServerUrl() == null) {
            error = AccountManagementLocalize.errorServerRequired();
        }
        if (error != LocalizeValue.empty()) {
            showError(error);
            return false;
        }

        myBusy = true;
        showError(AccountManagementLocalize.labelLoggingIn());
        session.login(myType, myConfig, getServerUrl(), getScopes()).whenCompleteAsync((result, throwable) -> {
            myBusy = false;
            if (throwable != null || result == null) {
                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                    ? throwable.getCause()
                    : throwable;
                String message = cause == null ? null : cause.getMessage();
                showError(AccountManagementLocalize.errorLoginFailed(StringUtil.notNullize(message, String.valueOf(cause))));
                return;
            }
            myResult = result;
            Dialog dialog = myDialog;
            if (dialog != null) {
                dialog.doOkAction(getOkValue());
            }
        }, UIAccess.current());
        return false;
    }

    private final class Context implements AccountAuthorizeContext {
        private final Disposable myDisposable;

        private Context(Disposable disposable) {
            myDisposable = disposable;
        }

        @Override
        public AccountType<?> getAccountType() {
            return myType;
        }

        @Override
        public AccountAuthorizeConfig getConfig() {
            return myConfig;
        }

        @Override
        public @Nullable String getServerUrl() {
            return AccountLoginDialogDescriptor.this.getServerUrl();
        }

        @Override
        public Set<? extends AccountScope> getScopes() {
            return AccountLoginDialogDescriptor.this.getScopes();
        }

        @Override
        public @Nullable Project getProject() {
            return myProject;
        }

        @Override
        public Disposable getDisposable() {
            return myDisposable;
        }

        @RequiredUIAccess
        @Override
        public void fireChanged() {
            showError(LocalizeValue.empty());
        }
    }
}
