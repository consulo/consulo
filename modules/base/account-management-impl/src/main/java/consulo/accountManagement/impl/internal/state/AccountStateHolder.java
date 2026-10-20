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
package consulo.accountManagement.impl.internal.state;

import consulo.component.persist.PersistentStateComponent;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public abstract class AccountStateHolder implements PersistentStateComponent<AccountListState> {
    private AccountListState myState = new AccountListState();

    @Override
    public AccountListState getState() {
        return myState;
    }

    @Override
    public void loadState(AccountListState state) {
        myState = state;
    }
}
