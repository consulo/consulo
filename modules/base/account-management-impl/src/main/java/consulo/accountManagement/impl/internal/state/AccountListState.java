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

import com.dslplatform.json.CompiledJson;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@CompiledJson
public class AccountListState {
    public @Nullable List<AccountEntry> ACCOUNTS = new ArrayList<>();
    public @Nullable Map<String, String> ACTIVE = new LinkedHashMap<>();

    public List<AccountEntry> accounts() {
        List<AccountEntry> accounts = ACCOUNTS;
        if (accounts == null) {
            accounts = new ArrayList<>();
            ACCOUNTS = accounts;
        }
        return accounts;
    }

    public Map<String, String> active() {
        Map<String, String> active = ACTIVE;
        if (active == null) {
            active = new LinkedHashMap<>();
            ACTIVE = active;
        }
        return active;
    }
}
