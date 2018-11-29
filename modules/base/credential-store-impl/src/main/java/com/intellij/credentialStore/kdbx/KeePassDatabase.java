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
package com.intellij.credentialStore.kdbx;

import com.intellij.credentialStore.CredentialStoreKt;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.NotNullLazyValue;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * from kotlin
 */
// we should on each save change protectedStreamKey for security reasons (as KeeWeb also does)
// so, this requirement (is it really required?) can force us to re-encrypt all passwords on save
public class KeePassDatabase {
  private Element rootElement;

  private NotNullLazyValue<SkippingStreamCipher> secureStringCipher =
          NotNullLazyValue.createValue(() -> KeePassDatabaseKt.createRandomlyInitializedChaCha7539Engine(CredentialStoreKt.createSecureRandom()));

  private volatile boolean isDirty;

  public KeePassDatabase() {
    this(KeePassDatabaseKt.createEmptyDatabase());
  }

  public StringProtectedByStreamCipher protectValue(@Nonnull String value) {
    return new StringProtectedByStreamCipher(value, secureStringCipher.getValue());
  }

  public KeePassDatabase(Element p) {
    this.rootElement = p;

    Element rootElement = JDOMUtil.getOrCreate(this.rootElement, KdbxDbElementNames.root);
    Element groupElement = rootElement.getChild(KdbxDbElementNames.group);
    if(groupElement == null) {
      roo
    }
  }

  public void setDirty(boolean dirty) {
  }

  public KdbxEntry createEntry(String title) {

  }

  public KdbxGroup getRootGroup() {

  }
}
