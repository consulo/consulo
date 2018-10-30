// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.passwordSafe;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PasswordStorage {
  /**
   * @deprecated Please use {@link #setPassword} and pass value as null
   */
  @SuppressWarnings("unused")
  @Deprecated
  default void removePassword(@SuppressWarnings("UnusedParameters") @Nullable Project project, @Nonnull Class requestor, String key) {
    set(new CredentialAttributes(requestor, key), null);
  }

  /**
   * @deprecated Please use {@link #setPassword}
   */
  @Deprecated
  default void storePassword(@SuppressWarnings("UnusedParameters") @Nullable Project project, @Nonnull Class requestor, @Nonnull String key, @Nullable String value) {
    set(new CredentialAttributes(requestor, key), value == null ? null : new Credentials(key, value));
  }

  @Deprecated
  @Nullable
  default String getPassword(@SuppressWarnings("UnusedParameters") @Nullable Project project, @Nonnull Class requestor, @Nonnull String key) {
    Credentials credentials = get(new CredentialAttributes(requestor, key));
    return credentials == null ? null : credentials.getPasswordAsString();
  }

  @Nullable
  Credentials get(@Nonnull CredentialAttributes attributes);

  void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials);
}
