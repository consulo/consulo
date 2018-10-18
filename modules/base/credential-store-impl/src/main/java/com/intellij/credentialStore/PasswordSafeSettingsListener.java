package com.intellij.credentialStore;

import com.intellij.ide.passwordSafe.impl.ProviderType;
import org.jetbrains.annotations.NotNull;

public interface PasswordSafeSettingsListener {
  default void typeChanged(@NotNull ProviderType oldValue, @NotNull ProviderType newValue) {
  }

  default void credentialStoreCleared() {
  }
}