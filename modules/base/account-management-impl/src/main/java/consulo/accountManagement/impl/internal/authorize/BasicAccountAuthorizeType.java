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

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountBasicCredentials;
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountRequestDecorator;
import consulo.accountManagement.authorize.BasicAuthorizeConfig;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.PasswordBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class BasicAccountAuthorizeType implements AccountAuthorizeType<BasicCredentials> {
    private static final char SEPARATOR = '\n';

    @Override
    public String getId() {
        return BasicAuthorizeConfig.ID;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.authorizeTypeBasic();
    }

    @RequiredUIAccess
    @Override
    public AccountAuthorizeForm<BasicCredentials> createForm(AccountAuthorizeContext context) {
        TextBox userBox = TextBox.create();
        PasswordBox passwordBox = PasswordBox.create();
        userBox.addValueListener(event -> context.fireChanged());
        passwordBox.addValueListener(event -> context.fireChanged());
        Component component = FormBuilder.create()
            .addLabeled(AccountManagementLocalize.labelLogin(), userBox)
            .addLabeled(AccountManagementLocalize.labelPassword(), passwordBox)
            .build();

        return new AccountAuthorizeForm<>() {
            @Override
            public Component getComponent() {
                return component;
            }

            @RequiredUIAccess
            @Override
            public @Nullable Component getPreferredFocusedComponent() {
                return userBox;
            }

            @RequiredUIAccess
            @Override
            public LocalizeValue validate() {
                return StringUtil.isEmptyOrSpaces(userBox.getValue()) ? AccountManagementLocalize.errorLoginRequired() : LocalizeValue.empty();
            }

            @RequiredUIAccess
            @Override
            public CompletableFuture<BasicCredentials> authorize() {
                return CompletableFuture.completedFuture(
                    new BasicCredentials(StringUtil.notNullize(userBox.getValue()).trim(), StringUtil.notNullize(passwordBox.getValue()))
                );
            }
        };
    }

    @Override
    public String serialize(BasicCredentials credentials) {
        return credentials.userName() + SEPARATOR + credentials.password();
    }

    @Override
    public BasicCredentials deserialize(String value) {
        int index = value.indexOf(SEPARATOR);
        return index < 0 ? new BasicCredentials(value, "") : new BasicCredentials(value.substring(0, index), value.substring(index + 1));
    }

    @Override
    public @Nullable String getUserName(BasicCredentials credentials) {
        return credentials.userName();
    }

    @Override
    public void decorate(BasicCredentials credentials, AccountAuthorizeConfig config, AccountRequestDecorator decorator) {
        String pair = credentials.userName() + ":" + credentials.password();
        decorator.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public AccountBasicCredentials getBasicCredentials(BasicCredentials credentials, AccountAuthorizeConfig config) {
        return new AccountBasicCredentials(credentials.userName(), credentials.password());
    }
}
