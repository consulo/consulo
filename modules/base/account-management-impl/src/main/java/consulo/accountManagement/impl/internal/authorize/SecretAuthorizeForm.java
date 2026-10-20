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
package consulo.accountManagement.impl.internal.authorize;

import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.PasswordBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
class SecretAuthorizeForm implements AccountAuthorizeForm<SecretCredentials> {
    private final PasswordBox myBox;
    private final Component myComponent;

    @RequiredUIAccess
    SecretAuthorizeForm(AccountAuthorizeContext context, LocalizeValue label, @Nullable Supplier<@Nullable String> newSecretUrl) {
        myBox = PasswordBox.create();
        myBox.addValueListener(event -> context.fireChanged());

        Component field = myBox;
        if (newSecretUrl != null) {
            Hyperlink generate = Hyperlink.create(AccountManagementLocalize.actionGenerateToken(), event -> {
                String url = newSecretUrl.get();
                if (url != null) {
                    BrowserUtil.browse(url);
                }
            });
            field = DockLayout.create().center(myBox).right(generate);
        }
        myComponent = FormBuilder.create().addLabeled(label, field).build();
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myBox;
    }

    @RequiredUIAccess
    @Override
    public LocalizeValue validate() {
        return StringUtil.isEmptyOrSpaces(myBox.getValue()) ? AccountManagementLocalize.errorSecretRequired() : LocalizeValue.empty();
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<SecretCredentials> authorize() {
        return CompletableFuture.completedFuture(new SecretCredentials(StringUtil.notNullize(myBox.getValue()).trim()));
    }
}
