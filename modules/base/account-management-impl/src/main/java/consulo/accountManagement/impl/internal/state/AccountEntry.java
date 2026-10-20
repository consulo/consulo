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
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@CompiledJson
public class AccountEntry {
    public String ID = "";
    public String TYPE = "";
    public String SERVER = "";
    public String NAME = "";
    public String LOGIN = "";
    public String AVATAR_URL = "";
    public String AUTHORIZE_TYPE = "";
    public String SOURCE = "";
    public @Nullable List<String> SCOPES = new ArrayList<>();

    public List<String> scopes() {
        List<String> scopes = SCOPES;
        if (scopes == null) {
            scopes = new ArrayList<>();
            SCOPES = scopes;
        }
        return scopes;
    }
}
