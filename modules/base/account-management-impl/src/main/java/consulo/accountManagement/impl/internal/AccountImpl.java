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
package consulo.accountManagement.impl.internal;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountLevel;
import consulo.accountManagement.impl.internal.state.AccountEntry;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class AccountImpl implements Account {
    private final AccountEntry myEntry;
    private volatile AccountLevel myLevel;
    private volatile boolean myAuthFailed;

    public AccountImpl(AccountEntry entry, AccountLevel level) {
        myEntry = entry;
        myLevel = level;
    }

    public AccountEntry getEntry() {
        return myEntry;
    }

    @Override
    public String getId() {
        return myEntry.ID;
    }

    @Override
    public String getTypeId() {
        return myEntry.TYPE;
    }

    @Override
    public @Nullable String getServerUrl() {
        return StringUtil.nullize(myEntry.SERVER);
    }

    @Override
    public String getName() {
        return myEntry.NAME;
    }

    @Override
    public @Nullable String getLogin() {
        return StringUtil.nullize(myEntry.LOGIN);
    }

    @Override
    public @Nullable String getAvatarUrl() {
        return StringUtil.nullize(myEntry.AVATAR_URL);
    }

    @Override
    public String getAuthorizeTypeId() {
        return myEntry.AUTHORIZE_TYPE;
    }

    @Override
    public String getCredentialSourceId() {
        return myEntry.SOURCE;
    }

    @Override
    public AccountLevel getLevel() {
        return myLevel;
    }

    void setLevel(AccountLevel level) {
        myLevel = level;
    }

    @Override
    public Set<String> getGrantedScopeIds() {
        return new LinkedHashSet<>(myEntry.scopes());
    }

    @Override
    public boolean isAuthFailed() {
        return myAuthFailed;
    }

    void setAuthFailed(boolean authFailed) {
        myAuthFailed = authFailed;
    }

    @Override
    public String toString() {
        return myEntry.TYPE + ":" + myEntry.NAME;
    }
}
