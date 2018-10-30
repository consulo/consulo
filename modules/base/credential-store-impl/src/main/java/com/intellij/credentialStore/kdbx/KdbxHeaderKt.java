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

import java.util.UUID;

/**
 * from kotlin
 */
public class KdbxHeaderKt {

  /**
   * This UUID denotes that AES Cipher is in use. No other values are known.
   */
  private static final UUID AES_CIPHER = UUID.fromString("31C1F2E6-BF71-4350-BE58-05216AFC5AFF");

  private static final int FILE_VERSION_CRITICAL_MASK = 0xFFFF0000;

  private static final int SIG1 = 0x9AA2D903;

  private static final int SIG2 = 0xB54BFB67;

  private static final int FILE_VERSION_32 = 0x00030001;
}
