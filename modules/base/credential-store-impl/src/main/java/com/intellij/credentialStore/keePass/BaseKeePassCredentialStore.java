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
package com.intellij.credentialStore.keePass;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialStore;
import com.intellij.credentialStore.Credentials;
import com.intellij.credentialStore.kdbx.KeePassDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * from kotlin
 */
public abstract class BaseKeePassCredentialStore implements CredentialStore {

  @Nullable
  @Override
  public Credentials get(@Nonnull CredentialAttributes attributes) {
    getDb().
    return null;
  }

  @Override
  public void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials) {

  }

  @Nonnull
  protected abstract KeePassDatabase getDb();

  protected abstract void markDirtry();
}
