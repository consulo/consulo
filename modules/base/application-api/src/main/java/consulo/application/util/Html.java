// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.util.collection.UnmodifiableHashMap;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

import java.util.*;

/**
 * An immutable representation of HTML node. Could be used as a DSL to quickly generate HTML strings.
 *
 * @see HtmlBuilder
 */
public abstract class Html {
    public abstract static class Chunk {
        /**
         * @return true if this chunk is empty (doesn't produce any text)
         */
        @Contract(pure = true)
        public boolean isEmpty() {
            return false;
        }

        /**
         * Appends the rendered HTML representation of this chunk to the supplied builder
         *
         * @param builder builder to append to.
         */
        public abstract void appendTo(@Nonnull StringBuilder builder);

        /**
         * @param tagName name of the tag to wrap with
         * @return an element that wraps this element
         */
        @Contract(pure = true)
        @Nonnull
        public Element wrapWith(@Nonnull String tagName) {
            return new Element(tagName, UnmodifiableHashMap.empty(), Collections.singletonList(this));
        }

        /**
         * @return the rendered HTML representation of this chunk.
         */
        @Override
        @Contract(pure = true)
        @Nonnull
        public String toString() {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            return builder.toString();
        }
    }

    private static class Empty extends Chunk {
        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
        }
    }

    private static class FormattedText extends Chunk {
        private final String myContent;

        private FormattedText(String content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            appendEscape(builder, myContent);
        }

        private static final List<String> FORMATTED_TEXT_FROM = List.of("<", ">", "&");
        private static final List<String> FORMATTED_TEXT_TO = List.of("&lt;", "&gt;", "&amp;");

        protected static void appendEscape(@Nonnull StringBuilder builder, String text) {
            StringUtil.appendReplacement(builder, text, FORMATTED_TEXT_FROM, FORMATTED_TEXT_TO);
        }
    }

    private static class PlainText extends Chunk {
        private final String myContent;

        private PlainText(String content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            appendEscape(builder, myContent);
        }

        private static final List<String> PLAIN_TEXT_FROM = List.of("<", ">", "&", "\n");
        private static final List<String> PLAIN_TEXT_TO = List.of("&lt;", "&gt;", "&amp;", "<br/>");

        protected static void appendEscape(@Nonnull StringBuilder builder, String text) {
            StringUtil.appendReplacement(builder, text, PLAIN_TEXT_FROM, PLAIN_TEXT_TO);
        }
    }

    private static class Raw extends Chunk {
        private final String myContent;

        private Raw(String content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(myContent);
        }
    }

    static class Fragment extends Chunk {
        private final List<Chunk> myContent;

        Fragment(List<Chunk> content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            for (Chunk chunk : myContent) {
                chunk.appendTo(builder);
            }
        }
    }

    private static class Nbsp extends Chunk {
        private static final Chunk ONE = new Nbsp(1);
        private final int myCount;

        private Nbsp(int count) {
            myCount = count;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(StringUtil.repeat("&nbsp;", myCount));
        }
    }

    public static class Element<THIS extends Element> extends Chunk {
        protected final String myTagName;
        protected final UnmodifiableHashMap<String, String> myAttributes;
        protected final List<Chunk> myChildren;

        private Element(String name, UnmodifiableHashMap<String, String> attributes, List<Chunk> children) {
            myTagName = name;
            myAttributes = attributes;
            myChildren = children;
        }

        @Contract(pure = true)
        @Nonnull
        @SuppressWarnings("unchecked")
        protected THIS withAttributes(UnmodifiableHashMap<String, String> newAttributes) {
            return (THIS)new Element(myTagName, newAttributes, myChildren);
        }

        @Contract(pure = true)
        @Nonnull
        @SuppressWarnings("unchecked")
        protected THIS withChildren(List<Chunk> newChildren) {
            return (THIS)new Element(myTagName, myAttributes, newChildren);
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append('<').append(myTagName);
            myAttributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                    entry -> {
                        builder.append(' ').append(entry.getKey()).append("=\"");
                        appendEscapeDoubleQuotedAttribute(builder, entry.getValue());
                        builder.append('"');
                    }
                );
            if (myChildren.isEmpty()) {
                builder.append("/>");
            }
            else {
                builder.append(">");
                for (Chunk child : myChildren) {
                    child.appendTo(builder);
                }
                builder.append("</").append(myTagName).append(">");
            }
        }

        /**
         * @param name  attribute name
         * @param value attribute value
         * @return a new element that is like this element but has the specified attribute added or replaced
         */
        @Contract(pure = true)
        @Nonnull
        public THIS attr(String name, String value) {
            return withAttributes(myAttributes.with(name, value));
        }

        @Contract(pure = true)
        @Nonnull
        public THIS attr(String name, int value) {
            return withAttributes(myAttributes.with(name, Integer.toString(value)));
        }

        /**
         * @param style CSS style specification
         * @return a new element that is like this element but has the specified style added or replaced
         */
        @Contract(pure = true)
        @Nonnull
        public THIS style(String style) {
            return attr("style", style);
        }

        /**
         * @param text localized text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public THIS addFormattedText(@Nonnull LocalizeValue text) {
            return child(formattedText(text));
        }

        /**
         * @param text localized text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public THIS addText(@Nonnull LocalizeValue text) {
            return child(text(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @Contract(pure = true)
        @Nonnull
        public THIS addText(@Nonnull String text) {
            return child(text(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public THIS addRaw(@Nonnull LocalizeValue text) {
            return child(raw(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public THIS addRaw(@Nonnull String text) {
            return child(raw(text));
        }

        /**
         * @param chunks chunks to add to the list of children
         * @return a new element that is like this element but has extra children
         */
        @Contract(pure = true)
        @Nonnull
        public THIS children(@Nonnull Chunk... chunks) {
            if (myChildren.isEmpty()) {
                return withChildren(Arrays.asList(chunks));
            }
            List<Chunk> newChildren = new ArrayList<>(myChildren.size() + chunks.length);
            newChildren.addAll(myChildren);
            Collections.addAll(newChildren, chunks);
            return withChildren(newChildren);
        }

        /**
         * @param chunks chunks to add to the list of children
         * @return a new element that is like this element but has extra children
         */
        @Contract(pure = true)
        @Nonnull
        public THIS children(@Nonnull List<Chunk> chunks) {
            if (myChildren.isEmpty()) {
                return withChildren(new ArrayList<>(chunks));
            }
            List<Chunk> newChildren = new ArrayList<>(myChildren.size() + chunks.size());
            newChildren.addAll(myChildren);
            newChildren.addAll(chunks);
            return withChildren(newChildren);
        }

        /**
         * @param chunk a chunk to add to the list of children
         * @return a new element that is like this element but has an extra child
         */
        @Contract(pure = true)
        @Nonnull
        public THIS child(@Nonnull Chunk chunk) {
            if (myChildren.isEmpty()) {
                return withChildren(Collections.singletonList(chunk));
            }
            List<Chunk> newChildren = new ArrayList<>(myChildren.size() + 1);
            newChildren.addAll(myChildren);
            newChildren.add(chunk);
            return withChildren(newChildren);
        }

        private static final List<String> DOUBLE_QUOTED_ATTR_FROM = List.of("<", ">", "&", "\"");
        private static final List<String> DOUBLE_QUOTED_ATTR_TO = List.of("&lt;", "&gt;", "&amp;", "&quot;");

        protected static void appendEscapeDoubleQuotedAttribute(@Nonnull StringBuilder builder, String text) {
            StringUtil.appendReplacement(builder, text, DOUBLE_QUOTED_ATTR_FROM, DOUBLE_QUOTED_ATTR_TO);
        }
    }

    public static class Anchor extends Element<Anchor> {
        public Anchor(String name, UnmodifiableHashMap<String, String> attributes, List<Chunk> children) {
            super(name, attributes, children);
        }

        @Nonnull
        @Override
        protected Anchor withAttributes(UnmodifiableHashMap<String, String> newAttributes) {
            return new Anchor(myTagName, newAttributes, myChildren);
        }

        @Nonnull
        @Override
        protected Anchor withChildren(List<Chunk> newChildren) {
            return new Anchor(myTagName, myAttributes, newChildren);
        }

        @Contract(pure = true)
        @Nonnull
        public Anchor href(String url) {
            return attr("href", url);
        }
    }

    public static class Img extends Element<Img> {
        public Img(String name, UnmodifiableHashMap<String, String> attributes, List<Chunk> children) {
            super(name, attributes, children);
        }

        @Nonnull
        @Override
        protected Img withAttributes(UnmodifiableHashMap<String, String> newAttributes) {
            return new Img(myTagName, newAttributes, myChildren);
        }

        @Nonnull
        @Override
        protected Img withChildren(List<Chunk> newChildren) {
            return new Img(myTagName, myAttributes, newChildren);
        }

        @Contract(pure = true)
        @Nonnull
        public Img height(int height) {
            return attr("height", height);
        }

        @Contract(pure = true)
        @Nonnull
        public Img width(int width) {
            return attr("width", width);
        }

        @Contract(pure = true)
        @Nonnull
        public Img src(String url) {
            return attr("src", url);
        }
    }

    /**
     * @param tagName name of the tag
     * @return an empty tag
     */
    @Contract(pure = true)
    @Nonnull
    public static Element tag(@Nonnull String tagName) {
        return new Element(tagName, UnmodifiableHashMap.empty(), Collections.emptyList());
    }

    /**
     * @return an &lt;a&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Anchor a() {
        return A;
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element body() {
        return BODY;
    }

    /**
     * @return a &lt;br&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element br() {
        return BR;
    }

    /**
     * @return a &lt;div&gt; element
     */
    @Contract(pure = true)
    @Nonnull
    public static Element div() {
        return DIV;
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element head() {
        return HEAD;
    }

    /**
     * @return a &lt;hr&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element hr() {
        return HR;
    }

    /**
     * @return a &lt;html&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element html() {
        return HTML;
    }

    /**
     * @return an &lt;img&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Img img() {
        return IMG;
    }

    /**
     * @return a &lt;li&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element li() {
        return LI;
    }


    /**
     * @return a &lt;p&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element p() {
        return P;
    }

    /**
     * @return a &lt;span&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element span() {
        return SPAN;
    }

    @Nonnull
    public static Element style(@Nonnull String style) {
        return STYLE.addRaw(style);
    }

    /**
     * @return a &lt;table&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element table() {
        return TABLE;
    }

    /**
     * @return a &lt;td&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element td() {
        return TD;
    }

    /**
     * @return a &lt;th&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element th() {
        return TH;
    }

    /**
     * @return a &lt;tr&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element tr() {
        return TR;
    }

    /**
     * @return a &lt;ul&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element ul() {
        return UL;
    }

    /**
     * Creates a HTML text node that represents a non-breaking space ({@code &nbsp;}).
     *
     * @return Chunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk nbsp() {
        return Nbsp.ONE;
    }

    /**
     * Creates a HTML text node that represents a given number of non-breaking spaces
     *
     * @param count number of non-breaking spaces
     * @return Chunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk nbsp(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException();
        }
        return new Nbsp(count);
    }

    /**
     * Creates a HTML pre-formatted text node
     *
     * @param text localized text to display (no escaping should be done by caller).
     * @return Chunk that represents a HTML text node.
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk formattedText(@Nonnull LocalizeValue text) {
        return text == LocalizeValue.empty() ? empty() : new FormattedText(text.get());
    }

    /**
     * Creates a HTML text node
     *
     * @param text localized text to display (no escaping should be done by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return Chunk that represents a HTML text node.
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk text(@Nonnull LocalizeValue text) {
        return text == LocalizeValue.empty() ? empty() : new PlainText(text.get());
    }

    /**
     * Creates a HTML text node
     *
     * @param text text to display (no escaping should be done by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return Chunk that represents a HTML text node.
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk text(@Nonnull String text) {
        return text.isEmpty() ? empty() : new PlainText(text);
    }

    /**
     * @return an empty Chunk
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk empty() {
        return Empty.INSTANCE;
    }

    /**
     * Creates a chunk that represents a piece of raw HTML. Should be used with care!
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return the Chunk that represents the supplied content.
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk raw(@Nonnull LocalizeValue rawHtml) {
        return rawHtml == LocalizeValue.empty() ? empty() : new Raw(rawHtml.get());
    }

    /**
     * Creates a chunk that represents a piece of raw HTML. Should be used with care!
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return the Chunk that represents the supplied content.
     */
    @Contract(pure = true)
    @Nonnull
    public static Chunk raw(@Nonnull String rawHtml) {
        return rawHtml.isEmpty() ? empty() : new Raw(rawHtml);
    }

    private static final Anchor A = new Anchor("a", UnmodifiableHashMap.empty(), Collections.emptyList());
    private static final Element BODY = tag("body");
    private static final Element BR = tag("br");
    private static final Element DIV = tag("div");
    private static final Element HEAD = tag("head");
    private static final Element HR = tag("hr");
    private static final Element HTML = tag("html");
    private static final Img IMG = new Img("img", UnmodifiableHashMap.empty(), Collections.emptyList());
    private static final Element LI = tag("li");
    private static final Element P = tag("p");
    private static final Element SPAN = tag("span");
    private static final Element STYLE = tag("style");
    private static final Element TABLE = tag("table");
    private static final Element TD = tag("td");
    private static final Element TH = tag("th");
    private static final Element TR = tag("tr");
    private static final Element UL = tag("ul");
}
