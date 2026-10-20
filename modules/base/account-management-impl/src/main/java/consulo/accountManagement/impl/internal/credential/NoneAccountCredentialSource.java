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
package consulo.accountManagement.impl.internal.credential;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountCredentialSource;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class NoneAccountCredentialSource implements AccountCredentialSource {
    @Override
    public String getId() {
        return AccountAuthorizeType.NONE_SOURCE;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.credentialSourceNone();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public CompletableFuture<@Nullable String> load(Account account) {
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<?> store(Account account, @Nullable String value) {
        return CompletableFuture.completedFuture(null);
    }
}
