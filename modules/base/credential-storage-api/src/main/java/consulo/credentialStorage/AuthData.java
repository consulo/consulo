/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.UsedInPlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Container for authentication data: login and password.
 * The password can be null intentionally: sometimes only login is known.
 *
 * @author Kirill Likhodedov
 */
@UsedInPlugin
public final class AuthData {
    @Nonnull
    private final String myLogin;
    @Nullable
    private final String myPassword;

    public AuthData(@Nonnull String login, @Nullable String password) {
        myPassword = password;
        myLogin = login;
    }

    @Nonnull
    public String getLogin() {
        return myLogin;
    }

    @Nullable
    public String getPassword() {
        return myPassword;
    }
}
