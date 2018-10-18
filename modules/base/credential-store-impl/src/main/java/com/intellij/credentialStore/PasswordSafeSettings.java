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
package com.intellij.credentialStore;

import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl;
import com.intellij.ide.passwordSafe.impl.PasswordSafeOptions;
import com.intellij.ide.passwordSafe.impl.ProviderType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.Topic;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * from kotlin
 */
@State(name = "PasswordSafe", storages = {@Storage(value = "security.xml", roamingType = RoamingType.DISABLED)})
public class PasswordSafeSettings implements PersistentStateComponent<PasswordSafeOptions> {
  public static final Topic<PasswordSafeSettingsListener> TOPIC = Topic.create("PasswordSafeSettingsListener", PasswordSafeSettingsListener.class);

  @Nonnull
  public static ProviderType getDefaultProviderType() {
    Platform.Info info = Platform.current().info();

    if (info.isWindows()) {
      return ProviderType.KEEPASS;
    }
    return ProviderType.KEYCHAIN;
  }

  private PasswordSafeOptions state = new PasswordSafeOptions();

  @Nonnull
  public ProviderType getProviderType() {
    if (Platform.current().info().isWindows()) {
      if (state.getProvider() == ProviderType.KEYCHAIN) {
        return ProviderType.KEEPASS;
      }
    }
    return state.getProvider();
  }

  @SuppressWarnings("deprecation")
  public void setProviderType(ProviderType newValue) {
    if (newValue == ProviderType.DO_NOT_STORE) {
      newValue = ProviderType.MEMORY_ONLY;
    }

    ProviderType oldValue = state.getProvider();

    if (!Objects.equals(newValue, oldValue)) {
      state.setProvider(newValue);

      Application.get().getMessageBus().syncPublisher(TOPIC).typeChanged(oldValue, newValue);
    }
  }

  public String getKeepassDb() {
    String result = state.getKeepassDb();
    if (result == null && getProviderType() == ProviderType.KEEPASS) {
      return PasswordSafeImpl.getDefaultKeePassDbFile().toString();
    }
    else {
      return result;
    }
  }

  public void setKeepassDb(String value) {
    String v = StringUtil.nullize(value, true);
    if (v != null && v.equals(PasswordSafeImpl.getDefaultKeePassDbFile().toString())) {
      v = null;
    }

    state.setKeepassDb(v);
  }

  @Nullable
  @Override
  public PasswordSafeOptions getState() {
    return state;
  }

  @Override
  public void loadState(PasswordSafeOptions state) {
    this.state = state;
    setProviderType(state.getProvider() == null ? getDefaultProviderType() : state.getProvider());
    state.setKeepassDb(StringUtil.nullize(state.getKeepassDb(), true));
  }
}
