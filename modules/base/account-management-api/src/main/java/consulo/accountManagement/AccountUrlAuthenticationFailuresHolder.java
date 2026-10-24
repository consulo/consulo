// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.accountManagement;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class AccountUrlAuthenticationFailuresHolder {
    private final Disposable myParentDisposable;
    private final Supplier<AccountManager> myAccountManager;
    private final ConcurrentMap<String, Set<String>> myStoreMap = new ConcurrentHashMap<>();
    private final AtomicBoolean myListening = new AtomicBoolean();

    public AccountUrlAuthenticationFailuresHolder(Disposable parentDisposable, Supplier<AccountManager> accountManager) {
        myParentDisposable = parentDisposable;
        myAccountManager = accountManager;
        Disposer.register(parentDisposable, myStoreMap::clear);
    }

    public void markFailed(Account account, String url) {
        if (myListening.compareAndSet(false, true)) {
            myAccountManager.get().addListener(new AccountListener() {
                @Override
                public void accountChanged(Account changed) {
                    if (!changed.isAuthFailed()) {
                        myStoreMap.remove(changed.getId());
                    }
                }
            }, myParentDisposable);
        }
        myStoreMap.computeIfAbsent(account.getId(), id -> ConcurrentHashMap.newKeySet()).add(url);
    }

    public boolean isFailed(Account account, String url) {
        Set<String> urls = myStoreMap.get(account.getId());
        return urls != null && urls.contains(url);
    }
}
