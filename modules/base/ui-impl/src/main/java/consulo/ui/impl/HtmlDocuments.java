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
package consulo.ui.impl;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class HtmlDocuments {
    private static final Pattern HEAD_START = Pattern.compile("<head(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_START = Pattern.compile("<html(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);

    private HtmlDocuments() {
    }

    public static String withHead(String html, String headContent) {
        if (headContent.isEmpty()) {
            return html;
        }

        Matcher head = HEAD_START.matcher(html);
        if (head.find()) {
            return html.substring(0, head.end()) + headContent + html.substring(head.end());
        }

        Matcher start = HTML_START.matcher(html);
        if (start.find()) {
            return html.substring(0, start.end()) + "<head>" + headContent + "</head>" + html.substring(start.end());
        }

        String lower = html.toLowerCase(Locale.ROOT);
        String body = lower.contains("<body") ? html : "<body>" + html + "</body>";
        return "<html><head>" + headContent + "</head>" + body + "</html>";
    }
}
