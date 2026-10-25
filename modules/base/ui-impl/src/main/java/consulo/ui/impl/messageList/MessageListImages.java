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
package consulo.ui.impl.messageList;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public final class MessageListImages {
    public static final int MAX_IMAGE_HEIGHT = 500;

    private static final Pattern HTML_IMAGE = Pattern.compile("(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])(.*?)\\2", Pattern.CASE_INSENSITIVE);

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("(!\\[[^\\]]*\\]\\(\\s*)(<[^>]*>|[^)\\s]+)");

    public static String replaceHtmlSources(String html, Function<String, String> replacement) {
        Matcher matcher = HTML_IMAGE.matcher(html);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String source = unescapeHtml(matcher.group(3));
            String quote = matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + quote + escapeHtml(replacement.apply(source)) + quote));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String replaceMarkdownSources(String markdown, Function<String, String> replacement) {
        Matcher matcher = MARKDOWN_IMAGE.matcher(markdown);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String source = matcher.group(2);
            boolean angled = source.startsWith("<") && source.endsWith(">");
            String url = angled ? source.substring(1, source.length() - 1) : source;
            String replaced = replacement.apply(url);
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + (angled ? "<" + replaced + ">" : replaced)));
        }
        matcher.appendTail(result);
        return replaceHtmlSources(result.toString(), replacement);
    }

    private static String unescapeHtml(String text) {
        return text.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private MessageListImages() {
    }
}
