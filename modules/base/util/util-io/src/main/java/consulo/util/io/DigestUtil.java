/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.io;

import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;

import java.math.BigInteger;
import java.security.*;
import java.util.function.Supplier;

/**
 * from kotlin
 */
public class DigestUtil {
    private static final Provider sunSecurityProvider = Security.getProvider("SUN");

    private static final Supplier<SecureRandom> ourRandom = LazyValue.notNull(SecureRandom::new);

    @Nonnull
    public static MessageDigest md5() {
        return getMessageDigest("MD5");
    }

    @Nonnull
    public static MessageDigest sha1() {
        return getMessageDigest("SHA-1");
    }

    @Nonnull
    public static MessageDigest sha256() {
        return getMessageDigest("SHA-256");
    }

    @Nonnull
    public static MessageDigest sha2_512() {
        return getMessageDigest("SHA-512");
    }

    @Nonnull
    public static String randomToken() {
        return new BigInteger(130, ourRandom.get()).toString(32);
    }

    private static MessageDigest getMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm, sunSecurityProvider);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
