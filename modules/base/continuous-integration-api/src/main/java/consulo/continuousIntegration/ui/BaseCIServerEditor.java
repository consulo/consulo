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
package consulo.continuousIntegration.ui;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.ui.AccountChooser;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.CIServerOptions;
import consulo.continuousIntegration.CIServerType;
import consulo.continuousIntegration.localize.ContinuousIntegrationLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Account chooser, provider rows ({@link #addCustomRows(FormBuilder)}) and a test button.
 *
 * @since 2026-11-09
 */
public class BaseCIServerEditor<O extends CIServerOptions> extends CIServerEditor {
    protected final Project myProject;
    protected final CIServerType<O> myType;
    protected final O myOptions;
    private final Consumer<O> myChangeListener;
    private @Nullable Component myPreferredFocusedComponent;

    public BaseCIServerEditor(Project project, CIServerType<O> type, O options, Consumer<O> changeListener) {
        myProject = project;
        myType = type;
        myOptions = options;
        myChangeListener = changeListener;
    }

    @RequiredUIAccess
    @Override
    public Component createComponent() {
        FormBuilder builder = FormBuilder.create();

        AccountManager accountManager = myProject.getApplication().getInstance(AccountManager.class);
        AccountType<?> accountType = accountManager.findAccountType(myType.getAccountTypeId());
        if (accountType == null) {
            builder.addLabeled(
                ContinuousIntegrationLocalize.serverLabelAccount(),
                Label.create(ContinuousIntegrationLocalize.serverMissingAccountType(myType.getAccountTypeId()))
            );
        }
        else {
            AccountChooser chooser =
                myProject.getApplication().getInstance(AccountComponentFactory.class).createChooser(accountType, myProject, this);
            String accountId = myOptions.accountId;
            chooser.setValue(accountId == null ? null : accountManager.findAccount(accountId, myProject));
            chooser.addValueListener(account -> accountChanged(accountManager, account));
            builder.addLabeled(ContinuousIntegrationLocalize.serverLabelAccount(), chooser.getComponent());
            myPreferredFocusedComponent = chooser.getComponent();
        }

        addCustomRows(builder);

        builder.addBottom(Button.create(ContinuousIntegrationLocalize.serverButtonTest(), event -> testConnection(event.getComponent())));
        return builder.build();
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myPreferredFocusedComponent;
    }

    @RequiredUIAccess
    protected void addCustomRows(FormBuilder builder) {
    }

    protected void accountChanged(AccountManager accountManager, @Nullable Account account) {
        myOptions.accountId = account == null ? null : account.getId();
        String serverUrl = account == null ? null : accountManager.getServerUrl(account);
        if (serverUrl != null) {
            myOptions.serverUrl = serverUrl;
        }
        fireChanged();
    }

    protected final void fireChanged() {
        myChangeListener.accept(myOptions);
    }

    protected CIServer createServerForTest() {
        return myType.createServer(myProject, myType.copyOptions(myOptions));
    }

    @RequiredUIAccess
    private void testConnection(Component component) {
        UIAccess uiAccess = UIAccess.current();
        createServerForTest().testConnection().whenCompleteAsync((result, error) -> {
            if (error == null) {
                MessageBoxes.okInfo(ContinuousIntegrationLocalize.serverTestSuccessful()).showAsync(component);
            }
            else {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                String message = StringUtil.notNullize(cause.getMessage(), cause.getClass().getSimpleName());
                MessageBoxes.okError(ContinuousIntegrationLocalize.serverTestFailed(message)).showAsync(component);
            }
        }, uiAccess);
    }
}
