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

import com.intellij.credentialStore.gpg.Pgp;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class PgpKeyEncryptionSupport implements EncryptionSupport {
  private EncryptionSpec myEncryptionSpec;

  public PgpKeyEncryptionSupport(EncryptionSpec encryptionSpec) {
    myEncryptionSpec = encryptionSpec;
  }

  @Override
  public byte[] encypt(byte[] data) {
    return new Pgp().encrypt(data, myEncryptionSpec.getPgpKeyId());
  }

  @Override
  public byte[] decrypt(byte[] data) {
    return new Pgp().decrypt(data);
  }
}
