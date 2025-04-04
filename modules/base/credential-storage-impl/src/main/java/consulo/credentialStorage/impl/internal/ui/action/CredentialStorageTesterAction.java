/*
 * Copyright 2013-2025 consulo.io
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
package consulo.credentialStorage.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.ui.internal.InternalActionGroup;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialAttributesUtil;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2025-04-04
 */
@ActionImpl(id = "Internal.CredentialStorageTester", parents = @ActionParentRef(@ActionRef(type = InternalActionGroup.class)))
public class CredentialStorageTesterAction extends DumbAwareAction {
    private static class CredentialStorageDialogDescriptor extends DialogDescriptor {

        private final PasswordSafe myPasswordSafe;

        public CredentialStorageDialogDescriptor(@Nonnull LocalizeValue title, PasswordSafe passwordSafe) {
            super(title);
            myPasswordSafe = passwordSafe;
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        public Component createCenterComponent(@Nonnull Disposable uiDisposable) {
            FormBuilder builder = FormBuilder.create();

            TextBox serviceBox = TextBox.create("Internal");

            TextBox userBox = TextBox.create();
            userBox.setValue("test@test.com");

            PasswordBox passwordBox = PasswordBox.create();
            passwordBox.setValue("1234");

            builder.addLabeled(LocalizeValue.localizeTODO("Service:"), serviceBox);
            builder.addLabeled(LocalizeValue.localizeTODO("User:"), userBox);
            builder.addLabeled(LocalizeValue.localizeTODO("Password:"), passwordBox);

            Button testButton = Button.create(LocalizeValue.localizeTODO("Set Password"), event -> {
                String user = userBox.getValueOrError();
                String password = passwordBox.getValueOrError();

                String serviceName = CredentialAttributesUtil.generateServiceName(serviceBox.getValue(), user);

                myPasswordSafe.set(new CredentialAttributes(serviceName, user), new Credentials(user, password));
            });


            Button fetchPassword = Button.create(LocalizeValue.localizeTODO("Get Password"), event -> {
                String user = userBox.getValueOrError();

                String serviceName = CredentialAttributesUtil.generateServiceName(serviceBox.getValue(), user);

                CredentialAttributes attributes = new CredentialAttributes(serviceName, user);

                Credentials credentials = myPasswordSafe.get(attributes);

                String password = credentials.getPasswordAsString();

                Alerts.okInfo(LocalizeValue.ofNullable(password)).showAsync(event.getComponent());
            });

            builder.addBottom(testButton);
            builder.addBottom(fetchPassword);

            return builder.build();
        }

        @Nonnull
        @Override
        public AnAction[] createActions(boolean inverseOrder) {
            return AnAction.EMPTY_ARRAY;
        }

        @Override
        public boolean hasBorderAtButtonLayout() {
            return false;
        }
    }

    private final DialogService myDialogService;
    private final PasswordSafe myPasswordSafe;

    @Inject
    public CredentialStorageTesterAction(DialogService dialogService, PasswordSafe passwordSafe) {
        super(LocalizeValue.localizeTODO("Credential Storage Tester"));

        myDialogService = dialogService;
        myPasswordSafe = passwordSafe;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        LocalizeValue action = e.getPresentation().getTextValue();

        Dialog dialog = myDialogService.build(e.getData(Project.KEY), new CredentialStorageDialogDescriptor(action, myPasswordSafe));

        dialog.showAsync();
    }
}
