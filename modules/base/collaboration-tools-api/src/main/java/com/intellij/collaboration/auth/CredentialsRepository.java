// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface CredentialsRepository<A extends Account, Cred> {
  /**
   * Checks whether the account manager can persist credentials.
   * If it cannot, one might need to notify the user of a way to
   * fix this. Returns {@code true} when the credentials repository is
   * able to write credentials to persistent storage, {@code false} otherwise.
   */
  @Nonnull
  Flow<Boolean> getCanPersistCredentials();

  /**
   * Attempts to persist credentials to some credential store.
   *
   * @param account     The account to store credentials for.
   * @param credentials The actual credentials to store.
   */
  @Nullable
  Object persistCredentials(@Nonnull A account, @Nullable Cred credentials, @Nonnull Continuation<? super kotlin.Unit> continuation);

  /**
   * Attempts to retrieve credentials from storage for the given account.
   * If credentials stored cannot be retrieved, this function errors. If
   * the credential store does not contain credentials for the given account,
   * this function returns {@code null}.
   */
  @Nullable
  Object retrieveCredentials(@Nonnull A account, @Nonnull Continuation<? super Cred> continuation);
}
