// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2025 consulo.io
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
package consulo.collaboration.auth;

import java.util.UUID;

/**
 * Base class to represent an account for some external system.
 * Properties are abstract to allow marking them with persistence annotations.
 * <p>
 * Generally supposed to be used as means of distinguishing multiple credentials from PSafe.
 *
 * @see #getId() an internal unique identifier of an account
 * @see #getName() short display name for an account to be shown to a user (login/username/email)
 */
public abstract class Account {

    public abstract String getId();

    public abstract String getName();

    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Account that)) return false;
        return getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
