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

import consulo.platform.Platform;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class EncryptionSupportKt {
  @Nonnull
  public static EncryptionType getDefaultEncryptionType() {
    if (Platform.current().info().isWindows()) {
      return EncryptionType.CRYPT_32;
    }
    return EncryptionType.BUILT_IN;
  }
}
