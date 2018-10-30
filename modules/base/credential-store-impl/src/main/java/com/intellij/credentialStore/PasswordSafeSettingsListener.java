package com.intellij.credentialStore;

import com.intellij.ide.passwordSafe.impl.ProviderType;
import javax.annotation.Nonnull;

public interface PasswordSafeSettingsListener {
  default void typeChanged(@Nonnull ProviderType oldValue, @Nonnull ProviderType newValue) {
  }

  default void credentialStoreCleared() {
  }
}