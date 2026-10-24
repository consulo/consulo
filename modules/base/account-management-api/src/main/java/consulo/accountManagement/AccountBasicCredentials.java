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

import org.jspecify.annotations.Nullable;

/**
 * A user name and password pair for transports which cannot take request headers - git over HTTP asks for a
 * password, and a hosting service accepts the account token in its place.
 *
 * @param userName the user name, or {@code null} when the service accepts any - the caller uses the account login
 * @param password the password or the token
 */
public record AccountBasicCredentials(@Nullable String userName, String password) {
}
