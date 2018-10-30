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
package com.intellij.credentialStore.gpg;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class PgpKey {
  private final String keyId;
  private final String userId;

  public PgpKey(String keyId, String userId) {
    this.keyId = keyId;
    this.userId = userId;
  }

  public String getKeyId() {
    return keyId;
  }

  public String getUserId() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PgpKey pgpKey = (PgpKey)o;
    return Objects.equals(keyId, pgpKey.keyId) && Objects.equals(userId, pgpKey.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyId, userId);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PgpKey{");
    sb.append("keyId='").append(keyId).append('\'');
    sb.append(", userId='").append(userId).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
