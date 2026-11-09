/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.task.ui;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.ui.AccountChooser;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryOptions;
import consulo.task.TaskRepositoryType;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
public class BaseRepositoryEditor<O extends TaskRepositoryOptions> extends TaskRepositoryEditor {
    protected final Project myProject;
    protected final TaskRepositoryType<O> myType;
    protected final O myOptions;
    private final Consumer<O> myChangeListener;

    private @Nullable Component myPreferredFocusedComponent;

    public BaseRepositoryEditor(Project project, TaskRepositoryType<O> type, O options, Consumer<O> changeListener) {
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
            builder.addLabeled(TaskLocalize.serversLabelAccount(), Label.create(TaskLocalize.serversMissingAccountType(myType.getAccountTypeId())));
        }
        else {
            AccountChooser chooser = myProject.getApplication().getInstance(AccountComponentFactory.class).createChooser(accountType, myProject, this);
            String accountId = myOptions.accountId;
            chooser.setValue(accountId == null ? null : accountManager.findAccount(accountId, myProject));
            chooser.addValueListener(account -> accountChanged(accountManager, account));
            builder.addLabeled(TaskLocalize.serversLabelAccount(), chooser.getComponent());
            myPreferredFocusedComponent = chooser.getComponent();
        }

        addCustomRows(builder);

        TextBox formatBox = TextBox.create(myOptions.commitMessageFormat);
        formatBox.setEnabled(myOptions.shouldFormatCommitMessage);
        formatBox.addValueListener(event -> {
            myOptions.commitMessageFormat = StringUtil.notNullize(event.getValue());
            fireChanged();
        });
        builder.addLabeled(TaskLocalize.serversLabelCommitMessageFormat(), formatBox);

        builder.addBottom(Label.create(TaskLocalize.serversLabelPlaceholders(createRepositoryForTest().getComment())));

        CheckBox formatCheckBox = CheckBox.create(TaskLocalize.serversCheckboxFormatCommitMessage(), myOptions.shouldFormatCommitMessage);
        formatCheckBox.addValueListener(event -> {
            myOptions.shouldFormatCommitMessage = event.getValue();
            formatBox.setEnabled(event.getValue());
            fireChanged();
        });
        builder.addBottom(formatCheckBox);

        CheckBox shareCheckBox = CheckBox.create(TaskLocalize.serversCheckboxShare(), myOptions.shared);
        shareCheckBox.addValueListener(event -> {
            myOptions.shared = event.getValue();
            fireChanged();
        });
        builder.addBottom(shareCheckBox);

        String advertiser = myType.getAdvertiser();
        if (advertiser != null) {
            builder.addBottom(Label.create(LocalizeValue.of(advertiser)));
        }

        builder.addBottom(Button.create(TaskLocalize.serversButtonTest(), event -> testConnection(event.getComponent())));
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

    protected TaskRepository createRepositoryForTest() {
        return myType.createRepository(myProject, myType.copyOptions(myOptions));
    }

    @RequiredUIAccess
    private void testConnection(Component component) {
        UIAccess uiAccess = UIAccess.current();
        TaskManager.getManager(myProject).testConnection(createRepositoryForTest()).whenCompleteAsync((result, error) -> {
            if (error == null) {
                MessageBoxes.okInfo(TaskLocalize.serversTestSuccessful()).showAsync(component);
            }
            else {
                String message = StringUtil.notNullize(error.getMessage(), error.getClass().getSimpleName());
                MessageBoxes.okError(TaskLocalize.serversTestFailed(message)).showAsync(component);
            }
        }, uiAccess);
    }
}
