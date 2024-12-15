// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.application.util.Html.Element;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple builder to create HTML fragments. It encapsulates a series of {@link Html} objects.
 */
public final class HtmlBuilder {
    private final List<Html.Chunk> myChunks = new ArrayList<>();

    /**
     * Appends a new chunk to this builder
     *
     * @param chunk chunk to append
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder append(@Nonnull Html.Chunk chunk) {
        if (!chunk.isEmpty()) {
            myChunks.add(chunk);
        }
        return this;
    }

    @Contract("_ -> this")
    public HtmlBuilder append(@Nonnull HtmlBuilder builder) {
        myChunks.addAll(builder.myChunks);
        return this;
    }

    /**
     * Appends a text chunk to this builder
     *
     * @param text text to append (must not be escaped by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder append(@Nonnull String text) {
        return append(Html.text(text));
    }

    /**
     * Appends a text chunk to this builder
     *
     * @param text localized text to append (must not be escaped by caller).
     *             All {@code '\n'} characters will be converted to {@code <br/>}
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder append(@Nonnull LocalizeValue text) {
        return append(Html.text(text.get()));
    }

    /**
     * Appends a raw html text to this builder. Should be used with care.
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml localized raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder appendRaw(@Nonnull LocalizeValue rawHtml) {
        return append(Html.raw(rawHtml.get()));
    }

    /**
     * Appends a raw html text to this builder. Should be used with care.
     * The purpose of this method is to be able to externalize the text with embedded link. E.g.:
     * {@code "Click <a href=\"...\">here</a> for details"}.
     *
     * @param rawHtml raw HTML content. It's the responsibility of the caller to balance tags and escape HTML entities.
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder appendRaw(@Nonnull String rawHtml) {
        return append(Html.raw(rawHtml));
    }

    /**
     * Appends a non-breaking space ({@code &nbsp;} entity).
     *
     * @return this builder
     */
    @Contract(" -> this")
    public HtmlBuilder nbsp() {
        return append(Html.nbsp());
    }

    /**
     * Appends a series of non-breaking spaces ({@code &nbsp;} entities).
     *
     * @param count number of non-breaking spaces to append
     * @return this builder
     */
    @Contract("_ -> this")
    public HtmlBuilder nbsp(int count) {
        return append(Html.nbsp(count));
    }

    /**
     * Appends a line-break ({@code <br/>}).
     *
     * @return this builder
     */
    @Contract(" -> this")
    public HtmlBuilder br() {
        return append(Html.br());
    }

    /**
     * Appends a horizontal-rule ({@code <hr/>}).
     *
     * @return this builder
     */
    @Contract(" -> this")
    public HtmlBuilder hr() {
        return append(Html.hr());
    }

    /**
     * Wraps this builder content with a specified tag
     *
     * @param tag name of the tag to wrap with
     * @return a new Element object that contains chunks from this builder
     */
    @Contract(pure = true)
    @Nonnull
    public Element wrapWith(@Nonnull String tag) {
        return Html.tag(tag).children(myChunks.toArray(new Html.Chunk[0]));
    }

    /**
     * @return true if no elements were added to this builder
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return myChunks.isEmpty();
    }

    /**
     * @return a fragment chunk that contains all the chunks of this builder.
     */
    public Html.Chunk toFragment() {
        return new Html.Fragment(new ArrayList<>(myChunks));
    }

    /**
     * @return a rendered HTML representation of all the chunks in this builder.
     */
    @Override
    @Contract(pure = true)
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Html.Chunk chunk : myChunks) {
            chunk.appendTo(sb);
        }
        return sb.toString();
    }
}
