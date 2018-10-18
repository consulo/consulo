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

import com.intellij.credentialStore.PasswordSafeSettings;
import com.intellij.util.xmlb.annotations.OptionTag;

import java.util.Objects;

/**
* @author VISTALL
* @since 2018-10-12
*/
public class PasswordSafeOptions {
  @OptionTag("PROVIDER")
  private ProviderType provider = PasswordSafeSettings.getDefaultProviderType();

  private String keepassDb;

  private boolean isRememberPasswordByDefault;

  public ProviderType getProvider() {
    return provider;
  }

  public void setProvider(ProviderType provider) {
    this.provider = provider;
  }

  public String getKeepassDb() {
    return keepassDb;
  }

  public void setKeepassDb(String keepassDb) {
    this.keepassDb = keepassDb;
  }

  public boolean isRememberPasswordByDefault() {
    return isRememberPasswordByDefault;
  }

  public void setRememberPasswordByDefault(boolean rememberPasswordByDefault) {
    isRememberPasswordByDefault = rememberPasswordByDefault;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PasswordSafeOptions that = (PasswordSafeOptions)o;
    return isRememberPasswordByDefault == that.isRememberPasswordByDefault && provider == that.provider && Objects.equals(keepassDb, that.keepassDb);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, keepassDb, isRememberPasswordByDefault);
  }
}
