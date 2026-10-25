// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.accountManagement.oauth;

import consulo.util.io.DigestUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PkceUtils {
    public static String generateCodeVerifier() {
        return DigestUtil.randomToken();
    }

    public static String generateShaCodeChallenge(String codeVerifier, Base64.Encoder encoder) {
        byte[] sha = DigestUtil.sha256().digest(codeVerifier.getBytes(StandardCharsets.UTF_8));

        return encoder.encodeToString(sha);
    }

    private PkceUtils() {
    }
}
