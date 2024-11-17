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
public abstract class Html {
    private static class Empty extends Html {
        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
        }
    }

    private static class Text extends Html {
        private final String myContent;

        private Text(String content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(StringUtil.escapeXmlEntities(myContent).replaceAll("\n", "<br/>"));
        }
    }

    private static class Raw extends Html {
        private final String myContent;

        private Raw(String content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(myContent);
        }
    }

    static class Fragment extends Html {
        private final List<Html> myContent;

        Fragment(List<Html> content) {
            myContent = content;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            for (Html chunk : myContent) {
                chunk.appendTo(builder);
            }
        }
    }

    private static class Nbsp extends Html {
        private static final Html ONE = new Nbsp(1);
        private final int myCount;

        private Nbsp(int count) {
            myCount = count;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append(StringUtil.repeat("&nbsp;", myCount));
        }
    }

    public static class Element extends Html {
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
        private final List<Html> myChildren;

        private Element(String name, UnmodifiableHashMap<String, String> attributes, List<Html> children) {
            myTagName = name;
            myAttributes = attributes;
            myChildren = children;
        }

        @Override
        public void appendTo(@Nonnull StringBuilder builder) {
            builder.append('<').append(myTagName);
            myAttributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                    entry -> builder.append(' ').append(entry.getKey()).append("=\"")
                        .append(StringUtil.escapeXmlEntities(entry.getValue())).append('"')
                );
            if (myChildren.isEmpty()) {
                builder.append("/>");
            }
            else {
                builder.append(">");
                for (Html child : myChildren) {
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
        public Element attr(String name, String value) {
            return new Element(myTagName, myAttributes.with(name, value), myChildren);
        }

        @Contract(pure = true)
        @Nonnull
        public Element attr(String name, int value) {
            return new Element(myTagName, myAttributes.with(name, Integer.toString(value)), myChildren);
        }

        /**
         * @param style CSS style specification
         * @return a new element that is like this element but has the specified style added or replaced
         */
        @Contract(pure = true)
        @Nonnull
        public Element style(String style) {
            return attr("style", style);
        }

        /**
         * @param text localized text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addText(@Nonnull LocalizeValue text) {
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
        public Element addText(@Nonnull String text) {
            return child(text(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addRaw(@Nonnull LocalizeValue text) {
            return child(raw(text));
        }

        /**
         * @param text text to add to the list of children (should not be escaped)
         * @return a new element that is like this element but has an extra text child
         */
        @Contract(pure = true)
        @Nonnull
        public Element addRaw(@Nonnull String text) {
            return child(raw(text));
        }

        /**
         * @param chunks chunks to add to the list of children
         * @return a new element that is like this element but has extra children
         */
        @Contract(pure = true)
        @Nonnull
        public Element children(@Nonnull Html... chunks) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, Arrays.asList(chunks));
            }
            List<Html> newChildren = new ArrayList<>(myChildren.size() + chunks.length);
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
        public Element children(@Nonnull List<Html> chunks) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, new ArrayList<>(chunks));
            }
            List<Html> newChildren = new ArrayList<>(myChildren.size() + chunks.size());
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
        public Element child(@Nonnull Html chunk) {
            if (myChildren.isEmpty()) {
                return new Element(myTagName, myAttributes, Collections.singletonList(chunk));
            }
            List<Html> newChildren = new ArrayList<>(myChildren.size() + 1);
            newChildren.addAll(myChildren);
            newChildren.add(chunk);
            return new Element(myTagName, myAttributes, newChildren);
        }
    }

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
     * @param element element to wrap with
     * @return an element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    public Element wrapWith(@Nonnull Element element) {
        return element.child(this);
    }

    /**
     * @return a CODE element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    public Element code() {
        return wrapWith("code");
    }

    /**
     * @return a B element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    public Element bold() {
        return wrapWith("b");
    }

    /**
     * @return an I element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    public Element italic() {
        return wrapWith("i");
    }

    /**
     * @return an S element that wraps this element
     */
    @Contract(pure = true)
    @Nonnull
    public Element strikethrough() {
        return wrapWith("s");
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
     * @return a &lt;div&gt; element
     */
    @Contract(pure = true)
    @Nonnull
    public static Element div() {
        return Element.DIV;
    }

    /**
     * @return a &lt;div&gt; element with a specified style.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element div(@Nonnull String style) {
        return Element.DIV.style(style);
    }

    /**
     * @return a &lt;span&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element span() {
        return Element.SPAN;
    }

    /**
     * @return a &lt;span&gt; element with a specified style.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element span(@Nonnull String style) {
        return Element.SPAN.style(style);
    }

    /**
     * @return a &lt;br&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element br() {
        return Element.BR;
    }

    /**
     * @return a &lt;li&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element li() {
        return Element.LI;
    }

    /**
     * @return a &lt;ul&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element ul() {
        return Element.UL;
    }

    /**
     * @return a &lt;hr&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element hr() {
        return Element.HR;
    }

    /**
     * @return a &lt;p&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element p() {
        return Element.P;
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element head() {
        return Element.HEAD;
    }

    @Nonnull
    public static Element styleTag(@Nonnull String style) {
        return tag("style").addRaw(style); //NON-NLS
    }

    @Nonnull
    public static Element font(@Nonnull String color) {
        return tag("font").attr("color", color);
    }

    /**
     * @return a &lt;body&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element body() {
        return Element.BODY;
    }

    /**
     * @return a &lt;html&gt; element.
     */
    @Contract(pure = true)
    @Nonnull
    public static Element html() {
        return Element.HTML;
    }

    /**
     * Creates a HTML text node that represents a non-breaking space ({@code &nbsp;}).
     *
     * @return HtmlChunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    public static Html nbsp() {
        return Nbsp.ONE;
    }

    /**
     * Creates a HTML text node that represents a given number of non-breaking spaces
     *
     * @param count number of non-breaking spaces
     * @return HtmlChunk that represents a sequence of non-breaking spaces
     */
    @Contract(pure = true)
    @Nonnull
    public static Html nbsp(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException();
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
    public static Html text(@Nonnull LocalizeValue text) {
        return text == LocalizeValue.empty() ? empty() : new Text(text.get());
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
    public static Html text(@Nonnull String text) {
        return text.isEmpty() ? empty() : new Text(text);
    }

    /**
     * @return an empty HtmlChunk
     */
    @Contract(pure = true)
    @Nonnull
    public static Html empty() {
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
    public static Html raw(@Nonnull LocalizeValue rawHtml) {
        return rawHtml == LocalizeValue.empty() ? empty() : new Raw(rawHtml.get());
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
    public static Html raw(@Nonnull String rawHtml) {
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
    public static Element link(@Nonnull String target, @Nonnull LocalizeValue text) {
        return new Element("a", UnmodifiableHashMap.<String, String>empty().with("href", target), Collections.singletonList(text(text)));
    }

    /**
     * Creates an element that represents a simple HTML link.
     *
     * @param target link target (HREF)
     * @param text   link text
     * @return the Element that represents a link
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Contract(pure = true)
    @Nonnull
    public static Element link(@Nonnull String target, @Nonnull String text) {
        return link(target, LocalizeValue.of(text));
    }

    /**
     * Creates an html entity (e.g. `&ndash;`)
     *
     * @param htmlEntity entity
     * @return the HtmlChunk that represents the html entity
     */
    @Contract(pure = true)
    @Nonnull
    public static Html htmlEntity(@Nonnull String htmlEntity) {
        return raw(htmlEntity);
    }

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

    /**
     * @return the collector that collects a stream of HtmlChunks to the fragment chunk.
     */
    @Contract(pure = true)
    @Nonnull
    public static Collector<Html, ?, Html> toFragment() {
        return Collector.of(HtmlBuilder::new, HtmlBuilder::append, HtmlBuilder::append, HtmlBuilder::toFragment);
    }

    /**
     * @param separator a chunk that should be used as a delimiter
     * @return the collector that collects a stream of HtmlChunks to the fragment chunk.
     */
    @Contract(pure = true)
    @Nonnull
    public static Collector<Html, ?, Html> toFragment(Html separator) {
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
}
