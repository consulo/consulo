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

import consulo.accountManagement.AccountManager;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Stored accounts of a type from a plugin loaded at runtime become known, and unknown again after it is unloaded.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class AccountDynamicPluginListener implements DynamicPluginListener {
    private final Provider<AccountManager> myAccountManager;

    @Inject
    public AccountDynamicPluginListener(Provider<AccountManager> accountManager) {
        myAccountManager = accountManager;
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        ((AccountManagerImpl) myAccountManager.get()).fireAccountsChanged();
    }
}
