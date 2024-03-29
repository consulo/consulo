/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.credentialStorage.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.inject.Singleton;

/**
 * The password safe settings
 */

/**
 * The password database. The internal component for {@link consulo.ide.impl.idea.ide.passwordSafe.impl.providers.masterKey.MasterKeyPasswordSafe}.
 */
@Singleton
@State(name = "PasswordSafe", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/security.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class PasswordSafeSettings implements PersistentStateComponent<PasswordSafeSettings.State> {
  /**
   * The selected provider type
   */
  private ProviderType myProviderType = ProviderType.MASTER_PASSWORD;

  /**
   * @return get provider type
   */
  public ProviderType getProviderType() {
    return myProviderType;
  }

  /**
   * Set provider type
   *
   * @param providerType a new value
   */
  public void setProviderType(ProviderType providerType) {
    myProviderType = providerType;
  }

  /**
   * {@inheritDoc}
   */
  public State getState() {
    State s = new State();
    s.PROVIDER = myProviderType;
    return s;
  }

  /**
   * {@inheritDoc}
   */
  public void loadState(State state) {
    myProviderType = state.PROVIDER;
  }


  /**
   * The settings state
   */
  public static class State {
    public ProviderType PROVIDER = ProviderType.MASTER_PASSWORD;
  }

  /**
   * The provider type for the password safe
   */
  public enum ProviderType {
    /**
     * The passwords are not stored
     */
    DO_NOT_STORE,
    /**
     * The passwords are stored only in the memory
     */
    MEMORY_ONLY,
    /**
     * The passwords are encrypted with master password
     */
    MASTER_PASSWORD
  }
}
