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
package consulo.externalService.impl.internal.auth;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountDetails;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.NoScope;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class HubAccountType implements AccountType<NoScope> {
    public static final String ID = "consuloHub";

    private final Application myApplication;

    @Inject
    public HubAccountType(Application application) {
        myApplication = application;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return ExternalServiceLocalize.hubAccountTypeName();
    }

    @Override
    public Image getIcon() {
        return myApplication.getIcon();
    }

    @Override
    public Class<NoScope> getScopeClass() {
        return NoScope.class;
    }

    @Override
    public List<? extends AccountAuthorizeConfig> getAuthorizeConfigs() {
        return List.of(HubAuthorizeConfig.INSTANCE);
    }

    @Override
    public CompletableFuture<AccountDetails> loadDetails(AccountConnection connection) {
        String userName = connection.getUserName();
        if (userName == null || userName.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No hub user"));
        }
        return CompletableFuture.completedFuture(new AccountDetails(userName, userName, gravatarUrl(userName)));
    }

    private static String gravatarUrl(String email) {
        return "https://www.gravatar.com/avatar/" + DigestUtils.md5Hex(email.toLowerCase(Locale.ROOT).trim()) + ".png?s=64&d=identicon";
    }
}
