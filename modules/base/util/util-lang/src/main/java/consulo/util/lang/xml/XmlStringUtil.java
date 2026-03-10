/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.util.lang.xml;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.StringUtil;
import consulo.util.lang.internal.Verifier;
import org.jetbrains.annotations.Contract;

import org.jspecify.annotations.Nullable;

import static consulo.util.lang.xml.CommonXmlStrings.*;

/**
 * @author yole
 */
public class XmlStringUtil {
    private XmlStringUtil() {
    }

    public static String wrapInCDATA(String str) {
        StringBuilder sb = new StringBuilder();
        int cur = 0, len = str.length();
        while (cur < len) {
            int next = StringUtil.indexOf(str, CDATA_END, cur);
            sb.append(CDATA_START).append(str.subSequence(cur, next = next < 0 ? len : next + 1)).append(CDATA_END);
            cur = next;
        }
        return sb.toString();
    }

    /**
     * Escapes characters in XML tag contents. The following replacements are performed:
     * <ul>
     * <li>{@code <} → {@code &lt;}</li>
     * <li>{@code >} → {@code &gt;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value a text to escape.
     * @return      the text with escapes.
     */
    public static String escapeText(CharSequence value) {
        return escapeText(value, 0, value.length());
    }

    /**
     * Escapes characters in XML tag contents. The following replacements are performed:
     * <ul>
     * <li>{@code <} → {@code &lt;}</li>
     * <li>{@code >} → {@code &gt;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value     a text to escape (only a part between {@code fromIndex} and {@code toIndex} will be used).
     * @param fromIndex an index where the text begins (included).
     * @param toIndex   an index where the text ends (excluded).
     * @return          the text with escapes.
     */
    @Contract(pure = true)
    public static String escapeText(CharSequence value, int fromIndex, int toIndex) {
        if (!needsTextEscapes(value, fromIndex, toIndex)) {
            return value.subSequence(fromIndex, toIndex).toString();
        }
        return escapeText(value, fromIndex, toIndex, new StringBuilder(value.length() + 10)).toString();
    }

    /**
     * Escapes characters in XML tag contents. The following replacements are performed:
     * <ul>
     * <li>{@code <} → {@code &lt;}</li>
     * <li>{@code >} → {@code &gt;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value   a text to escape.
     * @param builder a {@code StringBuilder} to append escaped text to.
     * @return        the {@code StringBuilder} from {@code builder} param after appending the text with escapes.
     */
    @Contract(mutates = "param2")
    public static StringBuilder escapeText(CharSequence value, StringBuilder builder) {
        return escapeText(value, 0, value.length(), builder);
    }

    /**
     * Escapes characters in XML tag contents. The following replacements are performed:
     * <ul>
     * <li>{@code <} → {@code &lt;}</li>
     * <li>{@code >} → {@code &gt;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value     a text to escape (only a part between {@code fromIndex} and {@code toIndex} will be used).
     * @param fromIndex an index where text begins (included).
     * @param toIndex   an index where text ends (excluded).
     * @param builder   a {@code StringBuilder} to append escaped text to.
     * @return          the {@code StringBuilder} from {@code builder} param after appending the text with escapes.
     */
    @Contract(mutates = "param4")
    public static StringBuilder escapeText(CharSequence value, int fromIndex, int toIndex, StringBuilder builder) {
        builder.ensureCapacity(builder.length() + toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '<' -> builder.append(LT);
                case '>' -> builder.append(GT);
                case '&' -> builder.append(AMP);
                default -> builder.append(ch);
            }
        }
        return builder;
    }

    /**
     * Escapes characters in XML tag attribute value. The following replacements are performed:
     * <ul>
     * <li>{@code "} → {@code &quot;} (if {@code quote} is {@code '"'})</li>
     * <li>{@code '} → {@code &apos;} (if {@code quote} is {@code '\''})</li>
     * <li>{@code \n} → {@code &#10;}</li>
     * <li>{@code \r} → {@code &#13;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value an attribute value to escape.
     * @return      the attribute value with escapes.
     */
    public static String escapeAttr(CharSequence value, char quote) {
        return escapeAttr(value, 0, value.length(), quote);
    }

    /**
     * Escapes characters in XML tag attribute value. The following replacements are performed:
     * <ul>
     * <li>{@code "} → {@code &quot;} (if {@code quote} is {@code '"'})</li>
     * <li>{@code '} → {@code &apos;} (if {@code quote} is {@code '\''})</li>
     * <li>{@code \n} → {@code &#10;}</li>
     * <li>{@code \r} → {@code &#13;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value     an attribute value to escape (only a part between {@code fromIndex} and {@code toIndex} will be used).
     * @param fromIndex an index where the attribute value begins (included).
     * @param toIndex   an index where the attribute value ends (excluded).
     * @param quote     a character used to surround attribute value.
     * @return          the attribute value with escapes.
     */
    @Contract(pure = true)
    public static String escapeAttr(CharSequence value, int fromIndex, int toIndex, char quote) {
        if (!needsAttrEscapes(value, fromIndex, toIndex, quote)) {
            return value.subSequence(fromIndex, toIndex).toString();
        }
        return escapeAttr(value, fromIndex, toIndex, quote, new StringBuilder(value.length() + 10)).toString();
    }

    /**
     * Escapes characters in XML tag attribute value. The following replacements are performed:
     * <ul>
     * <li>{@code "} → {@code &quot;} (if {@code quote} is {@code '"'})</li>
     * <li>{@code '} → {@code &apos;} (if {@code quote} is {@code '\''})</li>
     * <li>{@code \n} → {@code &#10;}</li>
     * <li>{@code \r} → {@code &#13;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value   an attribute value to escape.
     * @param quote   a character used to surround attribute value.
     * @param builder a {@code StringBuilder} to append escaped attribute value to.
     * @return        the {@code StringBuilder} from {@code builder} param after appending the attribute value with escapes.
     */
    @Contract(mutates = "param3")
    public static StringBuilder escapeAttr(CharSequence value, char quote, StringBuilder builder) {
        return escapeAttr(value, 0, value.length(), quote, builder);
    }

    /**
     * Escapes characters in XML tag attribute value. The following replacements are performed:
     * <ul>
     * <li>{@code "} → {@code &quot;} (if {@code quote} is {@code '"'})</li>
     * <li>{@code '} → {@code &apos;} (if {@code quote} is {@code '\''})</li>
     * <li>{@code \n} → {@code &#10;}</li>
     * <li>{@code \r} → {@code &#13;}</li>
     * <li>{@code &} → {@code &amp;}</li>
     * </ul>
     *
     * @param value     an attribute value to escape (only a part between {@code fromIndex} and {@code toIndex} will be used).
     * @param fromIndex an index where the attribute value begins (included).
     * @param toIndex   an index where the attribute value ends (excluded).
     * @param quote     a character used to surround attribute value.
     * @param builder   a {@code StringBuilder} to append escaped attribute value to.
     * @return          the {@code StringBuilder} from {@code builder} param after appending the attribute value with escapes.
     */
    @Contract(mutates = "param5")
    public static StringBuilder escapeAttr(
        CharSequence value,
        int fromIndex,
        int toIndex,
        char quote,
        StringBuilder builder
    ) {
        builder.ensureCapacity(builder.length() + toIndex - fromIndex);
        if (quote == '\'') {
            for (int i = fromIndex; i < toIndex; i++) {
                char ch = value.charAt(i);
                switch (ch) {
                    case '\n' -> builder.append("&#10;");
                    case '\r' -> builder.append("&#13;");
                    case '\'' -> builder.append(APOS);
                    case '&' -> builder.append(AMP);
                    default -> builder.append(ch);
                }
            }
        }
        else {
            for (int i = fromIndex; i < toIndex; i++) {
                char ch = value.charAt(i);
                switch (ch) {
                    case '\n' -> builder.append("&#10;");
                    case '\r' -> builder.append("&#13;");
                    case '"' -> builder.append(QUOT);
                    case '&' -> builder.append(AMP);
                    default -> builder.append(ch);
                }
            }
        }
        return builder;
    }

    @Contract(pure = true)
    private static boolean needsTextEscapes(CharSequence value, int fromIndex, int toIndex) {
        return StringUtil.indexOfAny(value, "<>&", fromIndex, toIndex) >= 0;
    }

    @Contract(pure = true)
    private static boolean needsAttrEscapes(CharSequence value, int fromIndex, int toIndex, char quote) {
        return StringUtil.indexOfAny(value, quote == '"' ? "\"\n\r&" : "'\n\r&", fromIndex, toIndex) >= 0;
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    @Deprecated
    @DeprecationInfo("Use #escapeText or #escapeAttr")
    @Nullable
    @SuppressWarnings("deprecation")
    public static String escapeString(@Nullable String str) {
        return escapeString(str, false);
    }

    @Contract(value = "null,_ -> null; !null,_ -> !null", pure = true)
    @Deprecated
    @DeprecationInfo("Use #escapeText or #escapeAttr")
    @Nullable
    @SuppressWarnings("deprecation")
    public static String escapeString(@Nullable String str, boolean escapeWhiteSpace) {
        return escapeString(str, escapeWhiteSpace, true);
    }

    @Contract(value = "null,_,_ -> null; !null,_,_ -> !null", pure = true)
    @Deprecated
    @DeprecationInfo("Use #escapeText or #escapeAttr")
    @Nullable
    @SuppressWarnings("deprecation")
    public static String escapeString(@Nullable String str, boolean escapeWhiteSpace, boolean convertNoBreakSpace) {
        if (str == null) {
            return null;
        }
        StringBuilder buffer = null;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            String entity = switch (ch) {
                case '\n' -> escapeWhiteSpace ? "&#10;" : null;
                case '\r' -> escapeWhiteSpace ? "&#13;" : null;
                case '\t' -> escapeWhiteSpace ? "&#9;" : null;
                case '\"' -> QUOT;
                case '<' -> LT;
                case '>' -> GT;
                case '&' -> AMP;
                case 160 -> // unicode char for &nbsp;
                    convertNoBreakSpace ? NBSP : null;
                default -> null;
            };
            if (buffer == null) {
                if (entity != null) {
                    // An entity occurred, so we'll have to use StringBuffer
                    // (allocate room for it plus a few more entities).
                    buffer = new StringBuilder(str.length() + 20);
                    // Copy previous skipped characters and fall through
                    // to pickup current character
                    buffer.append(str.substring(0, i));
                    buffer.append(entity);
                }
            }
            else if (entity == null) {
                buffer.append(ch);
            }
            else {
                buffer.append(entity);
            }
        }

        // If there were any entities, return the escaped characters
        // that we put in the StringBuffer. Otherwise, just return
        // the unmodified input string.
        return buffer == null ? str : buffer.toString();
    }

    public static String wrapInHtml(CharSequence result) {
        return HTML_START + result + HTML_END;
    }

    public static boolean isWrappedInHtml(String tooltip) {
        return StringUtil.startsWithIgnoreCase(tooltip, HTML_START) && StringUtil.endsWithIgnoreCase(tooltip, HTML_END);
    }

    public static String stripHtml(String toolTip) {
        toolTip = StringUtil.trimStart(toolTip, HTML_START);
        toolTip = StringUtil.trimStart(toolTip, BODY_START);
        toolTip = StringUtil.trimEnd(toolTip, HTML_END);
        toolTip = StringUtil.trimEnd(toolTip, BODY_END);
        return toolTip;
    }

    /**
     * Converts {@code text} to a string which can be used inside an HTML document: if it's already an HTML text the root html/body tags will
     * be stripped, if it's a plain text special characters will be escaped
     */
    public static String convertToHtmlContent(String text) {
        return isWrappedInHtml(text) ? stripHtml(text) : escapeText(text);
    }

    /**
     * Some characters are illegal in XML even as numerical character references. This method performs escaping of them
     * in a custom format, which is supposed to be unescaped on retrieving from XML using {@link #unescapeIllegalXmlChars(String)}.
     * Resulting text can be part of XML version 1.0 document.
     *
     * @see <a href="https://www.w3.org/International/questions/qa-controls">https://www.w3.org/International/questions/qa-controls</a>
     * @see Verifier#isXMLCharacter(int)
     */
    public static String escapeIllegalXmlChars(String text) {
        StringBuilder b = null;
        int lastPos = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(c)) {
                //noinspection AssignmentToForLoopParameter
                i++;
            }
            if (c == '#' || !Verifier.isXMLCharacter(c)) {
                if (b == null) {
                    b = new StringBuilder(text.length() + 5); // assuming there's one 'large' char (e.g. 0xFFFF) to escape numerically
                }
                b.append(text, lastPos, i).append('#');
                if (c != '#') {
                    b.append(Integer.toHexString(c));
                }
                b.append('#');
                lastPos = i + 1;
            }
        }
        return b == null ? text : b.append(text, lastPos, text.length()).toString();
    }

    /**
     * @see XmlStringUtil#escapeIllegalXmlChars(String)
     */
    public static String unescapeIllegalXmlChars(String text) {
        StringBuilder b = null;
        int lastPos = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c == '#') {
                int numberEnd = text.indexOf('#', i + 1);
                if (numberEnd > 0) {
                    int charCode;
                    try {
                        charCode = numberEnd == (i + 1) ? '#' : Integer.parseInt(text.substring(i + 1, numberEnd), 16);
                    }
                    catch (NumberFormatException e) {
                        continue;
                    }
                    if (b == null) {
                        b = new StringBuilder(text.length());
                    }
                    b.append(text, lastPos, i);
                    b.append((char) charCode);
                    //noinspection AssignmentToForLoopParameter
                    i = numberEnd;
                    lastPos = i + 1;
                }
            }
        }
        return b == null ? text : b.append(text, lastPos, text.length()).toString();
    }
}