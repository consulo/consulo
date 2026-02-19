// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.util.collection.UnmodifiableHashMap;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.stream.Collector;

/**
 * An immutable representation of HTML node. Could be used as a DSL to quickly generate HTML strings.
 *
 * @see HtmlBuilder
 */
public interface HtmlChunk {
    static final Collector<HtmlChunk, ?, HtmlChunk> FRAGMENT_COLLECTOR =
        Collector.of(HtmlBuilder::new, HtmlBuilder::append, HtmlBuilder::append, HtmlBuilder::toFragment);

    final class Empty implements HtmlChunk {
        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
        }

        @Nonnull
        @Override
        public String toString() {
            return "";
        }
    }

    static final record LocalizedRaw(@Nonnull LocalizeValue content) implements HtmlChunk {
        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(content.get());
        }

        @Nonnull
        @Override
        public String toString() {
            return content.get();
        }
    }

    static final record Raw(@Nonnull String content) implements HtmlChunk {
        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(content);
        }

        @Nonnull
        @Override
        public String toString() {
            return content;
        }
    }

    static final record Fragment(@Nonnull List<? extends HtmlChunk> content) implements HtmlChunk {
        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            for (HtmlChunk chunk : content) {
                chunk.appendTo(builder);
            }
        }

        @Nonnull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            return builder.toString();
        }
    }

    static final record Nbsp(int count) implements HtmlChunk {
        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(toString());
        }

        @Nonnull
        @Override
        public String toString() {
            return StringUtil.repeat("&nbsp;", count);
        }
    }

    class Element implements HtmlChunk {
        private static final Element HEAD = tag("head");
        private static final Element BODY = tag("body");
        private static final Element HTML = tag("html");
        private static final Element BR = tag("br");
        private static final Element UL = tag("ul");
        private static final Element LI = tag("li");
        private static final Element HR = tag("hr");
        private static final Element P = tag("p");
        private static final Element DIV = tag("div");
        private static final Element SPAN = tag("span");

        private final String myTagName;
        private final UnmodifiableHashMap<String, String> myAttributes;
        private final List<HtmlChunk> myChildren;

        private Element(String name, UnmodifiableHashMap<String, String> attributes, List<HtmlChunk> children) {
            myTagName = name;
            myAttributes = attributes;
            myChildren = children;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append('<').append(myTagName);
            myAttributes.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
                entry -> {
                    builder.append(' ').append(entry.getKey());
                    if (entry.getValue() != null) {
                        builder.append("=\"").append(StringUtil.escapeXmlEntities(entry.getValue())).append('"');
                    }
                }
            );
            if (myChildren.isEmpty()) {
                builder.append("/>");
            }
            else {
                builder.append(">");
                for (HtmlChunk child : myChildren) {
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
        public Element attr(@Nonnull String name, @Nonnull String value) {
            return new Element(myTagName, myAttributes.with(name, value), myChildren);
        }

        /**
         * @param name  attribute name
         * @param value attribute value
         * @return a new element that is like this element but has the specified attribute added or replaced
         */
        @Contract(pure = true)
        @Nonnull
        public Element attr(@Nonnull String name, int value) {
            return new Element(myTagName, myAttributes.with(name, Integer.toString(value)), myChildren);
        }

        /**
         * Adds an attribute without '=' sign and a value
         *
         * @param name attribute name
         * @return a new element that is like this element but has the specified attribute added or replaced
         */
        @Contract(pure = true)
        public @Nonnull Element attr(@Nonnull String name) {
            return new Element(myTagName, myAttributes.with(name, null), myChildren);
        }

        /**
         * @param style CSS style specification
         * @return a new element that is like this element but has the specified style added or replaced
         */
        @Contract(pure = true)
        @Nonnull
        public Element style(@Nonnull String style) {
            return attr("style", style);
        }

        /**
         * @param className name of style class
         * @return a new element that is like this element but has the specified class name
         */
        @Contract(pure = true)
        @Nonnull
        public Element setClass(@Nonnull String className) {
            return attr("class", className);
        }

        /**
         * @param text localized text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addText(@Nonnull LocalizeValue text) {
            return child(HtmlChunk.text(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @Contract(pure = true)
        @Nonnull
        public Element addText(@Nonnull String text) {
            return child(HtmlChunk.text(text));
        }

        /**
         * @param rawHtml text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addRaw(@Nonnull LocalizeValue rawHtml) {
            return child(HtmlChunk.raw(rawHtml));
        }

        /**
         * @param rawHtml text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addRaw(@Nonnull String rawHtml) {
            return child(HtmlChunk.raw(rawHtml));
        }

        /**
         * @param chunks chunks to add to the list of children
         * @return a new element that is like this element but has extra children
         */
        @Contract(pure = true)
        @Nonnull
        public Element children(@Nonnull HtmlChunk... chunks) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, Arrays.asList(chunks));
            }
            List<HtmlChunk> newChildren = new ArrayList<>(myChildren.size() + chunks.length);
            newChildren.addAll(myChildren);
            Collections.addAll(newChildren, chunks);
            return new Element(myTagName, myAttributes, newChildren);
        }

        /**
         * @param chunks chunks to add to the list of children
         * @return a new element that is like this element but has extra children
         */
        @Contract(pure = true)
        @Nonnull
        public Element children(@Nonnull List<HtmlChunk> chunks) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, new ArrayList<>(chunks));
            }
            List<HtmlChunk> newChildren = new ArrayList<>(myChildren.size() + chunks.size());
            newChildren.addAll(myChildren);
            newChildren.addAll(chunks);
            return new Element(myTagName, myAttributes, newChildren);
        }

        /**
         * @param chunk a chunk to add to the list of children
         * @return a new element that is like this element but has an extra child
         */
        @Contract(pure = true)
        @Nonnull
        public Element child(@Nonnull HtmlChunk chunk) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, Collections.singletonList(chunk));
            }
            List<HtmlChunk> newChildren = new ArrayList<>(myChildren.size() + 1);
            newChildren.addAll(myChildren);
            newChildren.add(chunk);
            return new Element(myTagName, myAttributes, newChildren);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Element element = (Element) o;
            return Objects.equals(myTagName, element.myTagName)
                && Objects.equals(myAttributes, element.myAttributes)
                && Objects.equals(myChildren, element.myChildren);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(myTagName);
            result = 31 * result + Objects.hashCode(myAttributes);
            return 31 * result + Objects.hashCode(myChildren);
        }

        @Contract(pure = true)
        @Override
        @Nonnull
        public String toString() {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            return builder.toString();
        }
    }

    static final HtmlChunk NBSP = raw("&nbsp;");

    /**
     * @param tagName name of the tag to wrap with
     * @return an element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element wrapWith(@Nonnull String tagName) {
        return new Element(tagName, UnmodifiableHashMap.empty(), Collections.singletonList(this));
    }

    /**
     * @param element element to wrap with
     * @return an element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element wrapWith(@Nonnull Element element) {
        return element.child(this);
    }

    /**
     * @return a CODE element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element code() {
        return wrapWith("code");
    }

    /**
     * @return a B element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element bold() {
        return wrapWith("b");
    }

    /**
     * @return an I element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element italic() {
        return wrapWith("i");
    }

    /**
     * @return an S element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    default Element strikethrough() {
        return wrapWith("s");
    }

    /**
     * @param tagName name of the tag
     * @return an empty tag
     */
    @Contract(pure = true)
    @Nonnull
    static Element tag(@Nonnull String tagName) {
        return new Element(tagName, UnmodifiableHashMap.empty(), Collections.emptyList());
    }

    /**
     * @return a &lt;div&gt; element
     */
    @Contract(pure = true)
    @Nonnull
    static Element div() {
        return Element.DIV;
    }

    /**
     * @return a &lt;div&gt; element with a specified style.
     */
    @Contract(pure = true)
    @Nonnull
    static Element div(@Nonnull String style) {
        return Element.DIV.style(style);
    }

    /**
     * @return a &lt;span&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element span() {
        return Element.SPAN;
    }

    /**
     * @return a &lt;span&gt; element with a specified style.
     */
    @Contract(pure = true)
    @Nonnull
    static Element span(@Nonnull String style) {
        return Element.SPAN.style(style);
    }

    /**
     * @return a &lt;br&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element br() {
        return Element.BR;
    }

    /**
     * @return a &lt;li&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element li() {
        return Element.LI;
    }

    /**
     * @return a &lt;ul&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element ul() {
        return Element.UL;
    }

    /**
     * @return a &lt;hr&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element hr() {
        return Element.HR;
    }

    /**
     * @return a &lt;p&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element p() {
        return Element.P;
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element head() {
        return Element.HEAD;
    }

    @Nonnull
    static Element styleTag(@Nonnull String style) {
        return tag("style").addRaw(style);
    }

    @Nonnull
    static Element font(@Nonnull String color) {
        return tag("font").attr("color", color);
    }

    @Nonnull
    static Element font(int size) {
        return tag("font").attr("size", String.valueOf(size));
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element body() {
        return Element.BODY;
    }

    /**
     * @return a &lt;html&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    static Element html() {
        return Element.HTML;
    }

    /**
     * Creates a HTML text node that represents a non-breaking space ({@code &nbsp;}).
     *
     * @return HtmlChunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk nbsp() {
        return NBSP;
    }

    /**
     * Creates a HTML text node that represents a given number of non-breaking spaces
     *
     * @param count number of non-breaking spaces
     * @return HtmlChunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk nbsp(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException(count + " is less than 0");
        }
        return new Nbsp(count);
    }

    /**
     * Creates a HTML text node
     *
     * @param text localized text to display (no escaping should be done by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return HtmlChunk that represents a HTML text node.
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk text(@Nonnull LocalizeValue text) {
        return text == LocalizeValue.empty() ? empty() : new LocalizedRaw(text.map(HtmlChunk::textToRaw));
    }

    /**
     * Creates a HTML text node
     *
     * @param text text to display (no escaping should be done by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return HtmlChunk that represents a HTML text node.
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk text(@Nonnull String text) {
        return text.isEmpty() ? empty() : new Raw(textToRaw(text));
    }

    /**
     * @return an empty HtmlChunk
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk empty() {
        return Empty.INSTANCE;
    }

    /**
     * Creates a chunk that represents a piece of raw HTML. Should be used with care!
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return the HtmlChunk that represents the supplied content.
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk raw(@Nonnull LocalizeValue rawHtml) {
        return rawHtml == LocalizeValue.empty() ? empty() : new LocalizedRaw(rawHtml);
    }

    /**
     * Creates a chunk that represents a piece of raw HTML. Should be used with care!
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return the HtmlChunk that represents the supplied content.
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk raw(@Nonnull String rawHtml) {
        return rawHtml.isEmpty() ? empty() : new Raw(rawHtml);
    }

    /**
     * Creates an element that represents a simple HTML link.
     *
     * @param target link target (HREF)
     * @param text   localized link text
     * @return the Element that represents a link
     */
    @Contract(pure = true)
    @Nonnull
    static Element link(@Nonnull String target, @Nonnull LocalizeValue text) {
        return new Element("a", UnmodifiableHashMap.<String, String>empty().with("href", target), Collections.singletonList(text(text)));
    }

    /**
     * Creates an element that represents an HTML link.
     *
     * @param target link target (HREF)
     * @param text   link text chunk
     * @return the Element that represents a link
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Contract(pure = true)
    @Nonnull
    static Element link(@Nonnull String target, @Nonnull String text) {
        return new Element("a", UnmodifiableHashMap.<String, String>empty().with("href", target), Collections.singletonList(text(text)));
    }

    /**
     * Creates an element that represents an HTML link.
     *
     * @param target link target (HREF)
     * @param text   link text chunk
     * @return the Element that represents a link
     */
    @Contract(pure = true)
    @Nonnull
    static Element link(@Nonnull String target, @Nonnull HtmlChunk text) {
        return new Element("a", UnmodifiableHashMap.<String, String>empty().with("href", target), Collections.singletonList(text));
    }

    /**
     * Creates an HTML entity (e.g. `&ndash;`)
     *
     * @param htmlEntity entity
     * @return the HtmlChunk that represents the html entity
     */
    @Contract(pure = true)
    @Nonnull
    static HtmlChunk htmlEntity(@Nonnull String htmlEntity) {
        if (!htmlEntity.startsWith("&") && !htmlEntity.endsWith(";")) {
            throw new IllegalArgumentException("Not an entity: " + htmlEntity);
        }
        return raw(htmlEntity);
    }

    /**
     * @return true if this chunk is empty (doesn't produce any text)
     */
    @Contract(pure = true)
    default boolean isEmpty() {
        return false;
    }

    /**
     * Appends the rendered HTML representation of this chunk to the supplied builder
     *
     * @param builder builder to append to.
     */
    void appendTo(@Nonnull StringBuilder builder);

    /**
     * @return the HtmlChunk that represents the fragment chunk
     */
    @Contract(pure = true)
    @Nonnull
    public static HtmlChunk fragment(@Nonnull HtmlChunk... chunks) {
        if (chunks.length == 0) {
            return empty();
        }
        return Arrays.stream(chunks).collect(toFragment());
    }

    /**
     * @return the collector that collects a stream of HtmlChunks to the fragment chunk.
     */
    @Contract(pure = true)
    @Nonnull
    static Collector<HtmlChunk, ?, HtmlChunk> toFragment() {
        return FRAGMENT_COLLECTOR;
    }

    /**
     * @param separator a chunk that should be used as a delimiter
     * @return the collector that collects a stream of HtmlChunks to the fragment chunk.
     */
    @Contract(pure = true)
    @Nonnull
    static Collector<HtmlChunk, ?, HtmlChunk> toFragment(HtmlChunk separator) {
        return Collector.of(
            HtmlBuilder::new,
            (hb, c) -> {
                if (!hb.isEmpty()) {
                    hb.append(separator);
                }
                hb.append(c);
            },
            (hb1, hb2) -> {
                if (!hb1.isEmpty()) {
                    hb1.append(separator);
                }
                return hb1.append(hb2);
            },
            HtmlBuilder::toFragment
        );
    }

    private static String textToRaw(@Nonnull String text) {
        return StringUtil.escapeXmlEntities(text).replaceAll("\n", "<br/>");
    }
}
