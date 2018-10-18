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
package com.intellij.ide.passwordSafe.impl;

import com.intellij.credentialStore.*;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.SettingsSavingComponent;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2018-10-12
 */
@Singleton
public class PasswordSafeImpl implements PasswordSafe, SettingsSavingComponent {
  @Nonnull
  public static Path getDefaultKeePassDbFile() {
    return KeePassCredentialStoreKt.getDefaultKeePassBaseDirectory().resolve(KeePassCredentialStoreKt.DB_FILE_NAME);
  }

  private final PasswordSafeSettings settings;
  private final CredentialStore provider;

  private final NotNullLazyValue<CredentialStore> _currentProvider;

  @Inject
  public PasswordSafeImpl(PasswordSafeSettings settings) {
    this(settings, null);
  }

  public PasswordSafeImpl(PasswordSafeSettings settings, CredentialStore provider) {

    this.settings = settings;
    this.provider = provider;

    _currentProvider = NotNullLazyValue.createValue(() -> {
      if (provider == null) {
        return computeProvider(settings);
      }
      else {
        return provider;
      }
    });
  }

  private static CredentialStore computeProvider(PasswordSafeSettings settings) {
     if(settings.getProviderType() == ProviderType.MEMORY_ONLY || Application.get().isUnitTestMode()) {
       return 
     }
  }

  @Override
  public boolean isRememberPasswordByDefault() {
    return false;
  }

  @Override
  public boolean isMemoryOnly() {
    return false;
  }

  @Override
  public void set(CredentialAttributes attributes, Credentials credentials, boolean memoryOnly) {

  }

  @Override
  public AsyncResult<Credentials> getAsync(CredentialAttributes attributes) {
    return null;
  }

  @Override
  public boolean isPasswordStoredOnlyInMemory(CredentialAttributes attributes, Credentials credentials) {
    return false;
  }

  @Nullable
  @Override
  public Credentials get(@NotNull CredentialAttributes attributes) {
    return null;
  }

  @Override
  public void set(@NotNull CredentialAttributes attributes, @Nullable Credentials credentials) {

  }

  @Override
  public void save() {

  }
}
