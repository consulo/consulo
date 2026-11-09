// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.client;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Locale;

record HttpContentType(String mimeType, @Nullable Charset charset) {
    /**
     * Parses a {@code Content-Type} header value: {@code type/subtype} followed by {@code ;}-separated parameters.
     *
     * @return {@code null} when there is no media type
     * @throws IllegalArgumentException when the charset parameter names an unknown or illegal charset
     */
    static @Nullable HttpContentType parse(String value) {
        String[] parts = value.split(";");
        String mimeType = parts[0].trim().toLowerCase(Locale.ROOT);
        if (mimeType.isEmpty()) {
            return null;
        }

        Charset charset = null;
        for (int i = 1; i < parts.length; i++) {
            String parameter = parts[i].trim();
            int eq = parameter.indexOf('=');
            if (eq <= 0 || !parameter.substring(0, eq).trim().equalsIgnoreCase("charset")) {
                continue;
            }
            String charsetName = unquote(parameter.substring(eq + 1).trim());
            if (!charsetName.isEmpty()) {
                charset = Charset.forName(charsetName);
            }
            break;
        }
        return new HttpContentType(mimeType, charset);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
