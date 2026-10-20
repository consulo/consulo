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
package consulo.sandboxPlugin.ide.account;

import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountRequestDecorator;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
final class SandAccountRequest implements AccountRequestDecorator {
    private final Map<String, String> myHeaders = new LinkedHashMap<>();
    private final Map<String, String> myParameters = new LinkedHashMap<>();

    static SandAccountRequest of(AccountConnection connection) {
        SandAccountRequest request = new SandAccountRequest();
        connection.decorate(request);
        return request;
    }

    @Override
    public void header(String name, String value) {
        myHeaders.put(name, value);
    }

    @Override
    public void queryParameter(String name, String value) {
        myParameters.put(name, value);
    }

    Map<String, String> getHeaders() {
        return myHeaders;
    }

    Map<String, String> getParameters() {
        return myParameters;
    }

    @Nullable String bearerToken() {
        String value = myHeaders.get("Authorization");
        return value != null && value.startsWith("Bearer ") ? value.substring("Bearer ".length()) : null;
    }

    @Override
    public String toString() {
        return "headers=" + myHeaders + ", parameters=" + myParameters;
    }
}
