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

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record LinkHttpHeaderValue(@Nullable String firstLink,
                                  @Nullable String prevLink,
                                  @Nullable String nextLink,
                                  @Nullable String lastLink,
                                  boolean isDeprecated) {
    public static final String HEADER_NAME = "Link";

    public static LinkHttpHeaderValue parse(String linkHeaderValue) {
        Map<String, String> headerElements = new HashMap<>();
        for (HeaderElement element : BasicHeaderValueParser.parseElements(linkHeaderValue, null)) {
            NameValuePair relParam = null;
            for (NameValuePair parameter : element.getParameters()) {
                if (parameter.getName().equals("rel")) {
                    relParam = parameter;
                    break;
                }
            }
            if (relParam == null) {
                throw new IllegalStateException("Missing rel-param in: '" + linkHeaderValue + "'");
            }
            if (element.getName() == null) {
                continue;
            }
            String urlPart = element.getName() + (element.getValue() == null ? "" : "=" + element.getValue());
            if (!urlPart.startsWith("<") || !urlPart.endsWith(">")) {
                throw new IllegalStateException("Invalid URL-part '" + urlPart + "' in: '" + linkHeaderValue + "'");
            }
            String url = urlPart.substring(1, urlPart.length() - 1);
            headerElements.put(relParam.getValue(), url);
        }
        return new LinkHttpHeaderValue(
            headerElements.get("first"),
            headerElements.get("prev"),
            headerElements.get("next"),
            headerElements.get("last"),
            headerElements.containsKey("deprecation")
        );
    }
}
