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

import com.intellij.credentialStore.OneTimeString;
import org.jdom.Text;

/**
 * @author VISTALL
 * @since 2018-10-27
 */
public class UnsavedProtectedValue extends Text implements SecureString {
  private final StringProtectedByStreamCipher secureString;

  public UnsavedProtectedValue(StringProtectedByStreamCipher secureString) {
    this.secureString = secureString;
  }

  public StringProtectedByStreamCipher getSecureString() {
    return secureString;
  }

  @Override
  public OneTimeString get(boolean clearable) {
    return secureString.get(clearable);
  }

  @Override
  public String getText() {
    throw new IllegalStateException("Must be converted to ProtectedValue for serialization");
  }
}
