// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class HtmlBuilderTest {
    @Test
    void isEmpty() {
        assertTrue(new HtmlBuilder().isEmpty());
        assertTrue(new HtmlBuilder().append(LocalizeValue.empty()).isEmpty());
        assertTrue(new HtmlBuilder().append("").isEmpty());
        assertTrue(new HtmlBuilder().appendRaw(LocalizeValue.empty()).isEmpty());
        assertTrue(new HtmlBuilder().appendRaw("").isEmpty());
        assertFalse(new HtmlBuilder().append("foo").isEmpty());
    }

    @Test
    void append() {
        assertThat(new HtmlBuilder().append("hello ").append("world!")).hasToString("hello world!");
        assertThat(new HtmlBuilder().append("hello ").append("world!")).hasToString("hello world!");
        assertThat(new HtmlBuilder().append(loc("<click here>"))).hasToString("&lt;click here&gt;");
        assertThat(new HtmlBuilder().append("<click here>")).hasToString("&lt;click here&gt;");
        assertThat(new HtmlBuilder().append(HtmlChunk.br()).append("<click here>")).hasToString("<br/>&lt;click here&gt;");
        assertThat(
            new HtmlBuilder().append("1")
                .append(new HtmlBuilder().append("2").append("3"))
                .append("4")
                .toString()
        ).isEqualTo("1234");
    }

    @Test
    void appendToItself() {
        HtmlBuilder builder = new HtmlBuilder();
        assertThatThrownBy(() -> builder.append(builder))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot add builder to itself");
    }

    @Test
    void appendLink() {
        assertThat(new HtmlBuilder().appendLink("url", loc("click")))
            .hasToString("<a href=\"url\">click</a>");
        assertThat(new HtmlBuilder().appendLink("url", "click"))
            .hasToString("<a href=\"url\">click</a>");
    }

    @Test
    void appendRaw() {
        assertThat(new HtmlBuilder().appendRaw(loc("<foo>"))).hasToString("<foo>");
        assertThat(new HtmlBuilder().appendRaw("<foo>")).hasToString("<foo>");
    }

    @Test
    void appendWithSeparators() {
        assertThat(new HtmlBuilder().appendWithSeparators(HtmlChunk.br(), Collections.emptyList())).hasToString("");
        String[] data = {"foo", "bar", "baz"};
        assertThat(new HtmlBuilder().appendWithSeparators(HtmlChunk.br(), ContainerUtil.map(data, d -> HtmlChunk.link(d, d))))
            .hasToString("<a href=\"foo\">foo</a><br/><a href=\"bar\">bar</a><br/><a href=\"baz\">baz</a>");
    }

    @Test
    void br() {
        assertThat(new HtmlBuilder().br()).hasToString("<br/>");
    }

    @Test
    void hr() {
        assertThat(new HtmlBuilder().hr()).hasToString("<hr/>");
    }

    @Test
    void nbsp() {
        assertThat(new HtmlBuilder().nbsp()).hasToString("&nbsp;");
        assertThat(new HtmlBuilder().nbsp(3)).hasToString("&nbsp;&nbsp;&nbsp;");
        assertThatThrownBy(() -> new HtmlBuilder().nbsp(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("-1 is less than 0");
    }

    @Test
    void wrapWith() {
        assertThat(new HtmlBuilder().append("Click ").appendLink("foo", "here").wrapWith("html"))
            .hasToString("<html>Click <a href=\"foo\">here</a></html>");
        assertThat(new HtmlBuilder().append("&&").wrapWith(HtmlChunk.div().style("color:blue")))
            .hasToString("<div style=\"color:blue\">&amp;&amp;</div>");
    }

    @Test
    void wrapWithHtmlBody() {
        assertThat(new HtmlBuilder().append("Hello").wrapWithHtmlBody())
            .hasToString("<html><body>Hello</body></html>");
    }

    @Test
    void toFragment() {
        HtmlBuilder builder = new HtmlBuilder();
        HtmlChunk fragment0 = builder.toFragment();
        builder.appendLink("1", "1");
        HtmlChunk fragment1 = builder.toFragment();
        builder.appendLink("2", "2");
        HtmlChunk fragment2 = builder.toFragment();
        assertThat(fragment0).hasToString("");
        assertThat(fragment1).hasToString("<a href=\"1\">1</a>");
        assertThat(fragment2).hasToString("<a href=\"1\">1</a><a href=\"2\">2</a>");
    }

    static LocalizeValue loc(@Nonnull String value) {
        return new TestLocalizedValue(value);
    }
}