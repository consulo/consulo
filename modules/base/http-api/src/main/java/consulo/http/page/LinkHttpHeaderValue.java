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
package consulo.http.page;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LinkHttpHeaderValue(@Nullable String firstLink,
                                  @Nullable String prevLink,
                                  @Nullable String nextLink,
                                  @Nullable String lastLink,
                                  boolean isDeprecated) {
    public static final String HEADER_NAME = "Link";

    public static LinkHttpHeaderValue parse(String linkHeaderValue) {
        Map<String, String> headerElements = new HashMap<>();
        for (String element : split(linkHeaderValue, ',')) {
            List<String> parts = split(element, ';');
            String urlPart = parts.get(0).trim();
            if (urlPart.isEmpty()) {
                continue;
            }

            String rel = null;
            for (int i = 1; i < parts.size(); i++) {
                String parameter = parts.get(i).trim();
                int eq = parameter.indexOf('=');
                String name = (eq < 0 ? parameter : parameter.substring(0, eq)).trim();
                if (name.equals("rel")) {
                    rel = eq < 0 ? "" : unquote(parameter.substring(eq + 1).trim());
                    break;
                }
            }
            if (rel == null) {
                throw new IllegalStateException("Missing rel-param in: '" + linkHeaderValue + "'");
            }
            if (!urlPart.startsWith("<") || !urlPart.endsWith(">")) {
                throw new IllegalStateException("Invalid URL-part '" + urlPart + "' in: '" + linkHeaderValue + "'");
            }
            headerElements.put(rel, urlPart.substring(1, urlPart.length() - 1));
        }
        return new LinkHttpHeaderValue(
            headerElements.get("first"),
            headerElements.get("prev"),
            headerElements.get("next"),
            headerElements.get("last"),
            headerElements.containsKey("deprecation")
        );
    }

    private static List<String> split(String value, char separator) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        boolean inUrl = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' && !inUrl) {
                inQuotes = !inQuotes;
            }
            else if (c == '<' && !inQuotes) {
                inUrl = true;
            }
            else if (c == '>' && !inQuotes) {
                inUrl = false;
            }
            else if (c == separator && !inQuotes && !inUrl) {
                result.add(value.substring(start, i));
                start = i + 1;
            }
        }
        result.add(value.substring(start));
        return result;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
