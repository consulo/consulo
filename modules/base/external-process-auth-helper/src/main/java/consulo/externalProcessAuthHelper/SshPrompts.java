// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalProcessAuthHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SshPrompts {
    public static final Pattern PASSPHRASE_PROMPT = Pattern.compile("\\r?Enter passphrase for( key)? '?(?<keyfile>[^']*)'?:\\s?");
    public static final Pattern PASSWORD_PROMPT = Pattern.compile("(?<username>.*)'s password:\\s?");
    public static final Pattern PKCS_PIN_TOKEN_PROMPT = Pattern.compile("\\r?Enter PIN for '?(?<tokenLabel>[^']*)'?:\\s?");
    public static final String PASSWORD_PROMPT_PREFIX = "password for";
    public static final String PASSWORD_PROMPT_SUFFIX = "password:";
    public static final String CONFIRM_CONNECTION_PROMPT = "Are you sure you want to continue connecting";
    public static final String REMOTE_HOST_IDENTIFICATION_HAS_CHANGED = "remote host identification has changed";

    private SshPrompts() {
    }

    public static String extractKeyPath(Matcher matcher) {
        return matcher.group("keyfile");
    }

    public static String extractPkcsTokenLabel(Matcher matcher) {
        return matcher.group("tokenLabel");
    }

    public static String extractUsername(Matcher matcher) {
        return matcher.group("username");
    }

    public static String stripConfirmConnectionOptions(String description) {
        // Are you sure you want to continue connecting (yes/no)?
        // Are you sure you want to continue connecting (yes/no/[fingerprint])?
        return description.replaceAll(CONFIRM_CONNECTION_PROMPT + " \\(.+?\\)\\?", CONFIRM_CONNECTION_PROMPT + "?");
    }
}
