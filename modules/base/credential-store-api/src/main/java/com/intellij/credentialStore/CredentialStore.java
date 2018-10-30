/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.credentialStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <a href="https://github.com/JetBrains/intellij-community/blob/master/platform/credential-store/readme.md">See documentation</a>
 */
public interface CredentialStore {
  @Nullable
  Credentials get(@Nonnull CredentialAttributes attributes);

  @Nullable
  default String getPassword(@Nonnull CredentialAttributes attributes) {
    Credentials credentials = get(attributes);
    return credentials == null ? null : credentials.getPasswordAsString();
  }

  void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials);

  default void setPassword(@Nonnull CredentialAttributes attributes, @Nullable String password) {
    set(attributes, password == null ? null : new Credentials(attributes.getUserName(), password));
  }
}
