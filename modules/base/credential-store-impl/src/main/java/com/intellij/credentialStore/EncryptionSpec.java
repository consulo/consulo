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

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class EncryptionSpec {
  private final EncryptionType myEncryptionType;
  @Nullable
  private final String myPgpKeyId;

  public EncryptionSpec(EncryptionType encryptionType, @Nullable String pgpKeyId) {
    myEncryptionType = encryptionType;
    myPgpKeyId = pgpKeyId;
  }

  public EncryptionType getEncryptionType() {
    return myEncryptionType;
  }

  @Nullable
  public String getPgpKeyId() {
    return myPgpKeyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EncryptionSpec that = (EncryptionSpec)o;
    return myEncryptionType == that.myEncryptionType && Objects.equals(myPgpKeyId, that.myPgpKeyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myEncryptionType, myPgpKeyId);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("EncryptionSpec{");
    sb.append("myEncryptionType=").append(myEncryptionType);
    sb.append(", myPgpKeyId='").append(myPgpKeyId).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
