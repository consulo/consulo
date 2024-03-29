/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.credentialStorage.impl.internal.provider;

import consulo.credentialStorage.PasswordSafeException;
import consulo.credentialStorage.impl.internal.PasswordSafeProvider;
import consulo.project.Project;
import jakarta.annotation.Nullable;

/**
 * The most secure provider that does not store anything, so it cannot be cracked
 */
public final class NilProvider extends PasswordSafeProvider {
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSupported() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "The provider that does not remembers password.";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "Do not Store";
  }

  /**
   * {@inheritDoc}
   */
  public String getPassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    // nothing is stored
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void removePassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  public void storePassword(@Nullable Project project, Class requester, String key, String value) throws PasswordSafeException {
    // just forget about password
  }
}
