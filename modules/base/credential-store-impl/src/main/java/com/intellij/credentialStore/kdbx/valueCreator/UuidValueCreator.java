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
package com.intellij.credentialStore.kdbx.valueCreator;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * from kotlin
 */
public class UuidValueCreator implements Supplier<String> {
  @Override
  public String get() {
    return base64FromUuid(UUID.randomUUID());
  }

  private String base64FromUuid(UUID uuid) {
    ByteBuffer b = ByteBuffer.wrap(new byte[16]);
    b.putLong(uuid.getMostSignificantBits());
    b.putLong(uuid.getLeastSignificantBits());
    return Base64.getEncoder().encodeToString(b.array());
  }
}
