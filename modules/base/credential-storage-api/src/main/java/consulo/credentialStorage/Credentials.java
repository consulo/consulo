/*
 * Copyright 2013-2024 consulo.io
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
package consulo.credentialStorage;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-03
 */
public record Credentials(@Nullable String user, @Nullable OneTimeString password) {
    public Credentials(String user) {
        this(user, (OneTimeString) null);
    }

    public Credentials(String user, String password) {
        this(user, password == null ? (OneTimeString) null : new OneTimeString(password));
    }

    @Nullable
    public String getPasswordAsString() {
        @Nullable OneTimeString password = password();
        if (password == null) {
            return null;
        }
        return password.toString();
    }
}
