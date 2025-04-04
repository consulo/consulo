/*
 * Copyright 2013-2025 consulo.io
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
package consulo.credentialStorage.impl.internal.kdbx;

import consulo.util.io.DigestUtil;

import java.util.Arrays;

/**
 * Implements KeePassCredentials by hashing the provided password twice using SHA-256.
 */
public class KdbxPassword implements KeePassCredentials {
    private final byte[] key;

    /**
     * Creates a KdbxPassword from the given password bytes.
     *
     * @param password the main password as a byte array
     */
    public KdbxPassword(byte[] password) {
        // Double-hash using SHA-256.
        byte[] hash1 = DigestUtil.sha256().digest(password);
        this.key = DigestUtil.sha256().digest(hash1);
    }

    /**
     * Creates a KdbxPassword and clears the original password array.
     *
     * @param value the main password as a byte array
     * @return a KeePassCredentials instance
     */
    public static KeePassCredentials createAndClear(byte[] value) {
        KdbxPassword result = new KdbxPassword(value);
        Arrays.fill(value, (byte) 0);
        return result;
    }

    @Override
    public byte[] getKey() {
        return key;
    }
}
