/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.ide.passwordSafe;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialStore;
import com.intellij.credentialStore.Credentials;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.AsyncResult;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-10-12
 */
public interface PasswordSafe extends PasswordStorage, CredentialStore {
  @Nonnull
  static PasswordSafe getInstance() {
    return ServiceManager.getService(PasswordSafe.class);
  }

  boolean isRememberPasswordByDefault();

  boolean isMemoryOnly();

  void set(CredentialAttributes attributes, Credentials credentials, boolean memoryOnly);

  AsyncResult<Credentials> getAsync(CredentialAttributes attributes);

  boolean isPasswordStoredOnlyInMemory(CredentialAttributes attributes, Credentials credentials);
}
