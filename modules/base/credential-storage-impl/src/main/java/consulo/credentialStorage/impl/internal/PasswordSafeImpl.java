/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.annotation.component.ServiceImpl;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.PasswordSafeException;
import consulo.credentialStorage.impl.internal.provider.MasterKeyPasswordSafe;
import consulo.credentialStorage.impl.internal.provider.MemoryPasswordSafe;
import consulo.credentialStorage.impl.internal.provider.NilProvider;
import consulo.credentialStorage.impl.internal.provider.masterKey.PasswordDatabase;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

/**
 * The implementation of password safe service
 */
@Singleton
@ServiceImpl
public class PasswordSafeImpl implements PasswordSafe {
  /**
   * The logger instance
   */
  private static final Logger LOG = Logger.getInstance(PasswordSafeImpl.class);
  /**
   * The current settings
   */
  private final PasswordSafeSettings mySettings;
  /**
   * The master key provider
   */
  private final MasterKeyPasswordSafe myMasterKeyProvider;
  /**
   * The nil provider
   */
  private final NilProvider myNilProvider;
  /**
   * The memory provider
   */
  private final MemoryPasswordSafe myMemoryProvider;

  /**
   * The constructor
   *
   * @param settings the settings for the password safe
   * @param database the password database
   */
  @Inject
  public PasswordSafeImpl(PasswordSafeSettings settings, PasswordDatabase database) {
    mySettings = settings;
    myMasterKeyProvider = new MasterKeyPasswordSafe(database);
    myNilProvider = new NilProvider();
    myMemoryProvider = new MemoryPasswordSafe();
  }

  /**
   * @return get currently selected provider
   */
  private PasswordSafeProvider provider() {
    PasswordSafeProvider p = null;
    switch (mySettings.getProviderType()) {
      case DO_NOT_STORE:
        p = myNilProvider;
        break;
      case MEMORY_ONLY:
        p = myMemoryProvider;
        break;
      case MASTER_PASSWORD:
        p = myMasterKeyProvider;
        break;
      default:
        LOG.error("Unknown provider type: " + mySettings.getProviderType());
    }
    if (p == null || !p.isSupported()) {
      p = myMemoryProvider;
    }
    return p;
  }


  /**
   * @return settings for the passwords safe
   */
  public PasswordSafeSettings getSettings() {
    return mySettings;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    if (mySettings.getProviderType().equals(PasswordSafeSettings.ProviderType.MASTER_PASSWORD)) {
      String password = getMemoryProvider().getPassword(project, requester, key);
      if (password == null) {
        password = provider().getPassword(project, requester, key);
        if (password != null) {
          // cache the password in memory as well for easier access during the session
          getMemoryProvider().storePassword(project, requester, key, password);
        }
      }
      return password;
    }
    return provider().getPassword(project, requester, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    if (mySettings.getProviderType().equals(PasswordSafeSettings.ProviderType.MASTER_PASSWORD)) {
      getMemoryProvider().removePassword(project, requester, key);
    }
    provider().removePassword(project, requester, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void storePassword(@Nullable Project project,
                            Class requester,
                            String key,
                            String value,
                            boolean recordPassword) throws PasswordSafeException {
    if (mySettings.getProviderType().equals(PasswordSafeSettings.ProviderType.MASTER_PASSWORD) && recordPassword) {
      getMemoryProvider().storePassword(project, requester, key, value);
    }
    provider().storePassword(project, requester, key, value);
  }

  /**
   * @return get master key provider instance (used for configuration specific to this provider)
   */
  public MasterKeyPasswordSafe getMasterKeyProvider() {
    return myMasterKeyProvider;
  }

  public MemoryPasswordSafe getMemoryProvider() {
    return myMemoryProvider;
  }
}
