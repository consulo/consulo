// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-02-18
 */
@SuppressWarnings("deprecation")
public class HtmlChunkTest {

    @Test
    void text() {
        assertThat(HtmlChunk.text("foo")).hasToString("foo");
        assertThat(HtmlChunk.text("<a href=\"hello\">")).hasToString("&lt;a href=&quot;hello&quot;&gt;");

        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.addText(loc("<foo>"))).hasToString("<p>&lt;foo&gt;</p>");
        assertThat(p.addText("<foo>")).hasToString("<p>&lt;foo&gt;</p>");
    }

    @Test
    void raw() {
        assertThat(HtmlChunk.raw("foo")).hasToString("foo");
        assertThat(HtmlChunk.raw("<a href=\"hello\">")).hasToString("<a href=\"hello\">");

        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.addRaw(loc("<foo>"))).hasToString("<p><foo></p>");
        assertThat(p.addRaw("<foo>")).hasToString("<p><foo></p>");
    }

    @Test
    void htmlEntity() {
        assertThat(HtmlChunk.htmlEntity("&foo;")).hasToString("&foo;");
        assertThatThrownBy(() -> HtmlChunk.htmlEntity("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Not an entity: foo");
    }

    @Test
    void nbsp() {
        assertThat(HtmlChunk.nbsp()).hasToString("&nbsp;");
        assertThat(HtmlChunk.nbsp(3)).hasToString("&nbsp;&nbsp;&nbsp;");
        assertThatThrownBy(() -> new HtmlBuilder().nbsp(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("-1 is less than 0");
    }

    @Test
    void tag() {
        assertThat(HtmlChunk.tag("b")).hasToString("<b/>");
    }

    @Test
    void body() {
        assertThat(HtmlChunk.body()).hasToString("<body/>");
    }

    @Test
    void br() {
        assertThat(HtmlChunk.br()).hasToString("<br/>");
    }

    @Test
    void div() {
        assertThat(HtmlChunk.div()).hasToString("<div/>");
        assertThat(HtmlChunk.div("color: blue")).hasToString("<div style=\"color: blue\"/>");
    }

    @Test
    void font() {
        assertThat(HtmlChunk.font(12)).hasToString("<font size=\"12\"/>");
        assertThat(HtmlChunk.font("blue")).hasToString("<font color=\"blue\"/>");
    }

    @Test
    void hr() {
        assertThat(HtmlChunk.hr()).hasToString("<hr/>");
    }

    @Test
    void head() {
        assertThat(HtmlChunk.head()).hasToString("<head/>");
    }

    @Test
    void html() {
        assertThat(HtmlChunk.html()).hasToString("<html/>");
    }

    @Test
    void li() {
        assertThat(HtmlChunk.li()).hasToString("<li/>");
    }

    @Test
    void link() {
        assertThat(HtmlChunk.link("target", loc("<Click here>")))
            .hasToString("<a href=\"target\">&lt;Click here&gt;</a>");
        assertThat(HtmlChunk.link("target", "<Click here>"))
            .hasToString("<a href=\"target\">&lt;Click here&gt;</a>");
        assertThat(HtmlChunk.link("target", HtmlChunk.text("<Click here>")))
            .hasToString("<a href=\"target\">&lt;Click here&gt;</a>");
    }

    @Test
    void p() {
        assertThat(HtmlChunk.p()).hasToString("<p/>");
    }

    @Test
    void span() {
        assertThat(HtmlChunk.span()).hasToString("<span/>");
        assertThat(HtmlChunk.span("color: blue")).hasToString("<span style=\"color: blue\"/>");
    }

    @Test
    void styleTag() {
        assertThat(HtmlChunk.styleTag("* {\ncolor: blue;\n}\n")).hasToString("<style>* {\ncolor: blue;\n}\n</style>");
    }

    @Test
    void ul() {
        assertThat(HtmlChunk.ul()).hasToString("<ul/>");
    }

    @Test
    void attr() {
        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.attr("align", "left")).hasToString("<p align=\"left\"/>");
        assertThat(p.attr("align", "left").attr("align", "right")).hasToString("<p align=\"right\"/>");
        assertThat(HtmlChunk.tag("img").attr("width", 16).attr("height", 16)).hasToString("<img height=\"16\" width=\"16\"/>");
        assertThat(HtmlChunk.tag("input").attr("disabled")).hasToString("<input disabled/>");
        assertThat(p.style("color: blue")).hasToString("<p style=\"color: blue\"/>");
        assertThat(p.setClass("invisible")).hasToString("<p class=\"invisible\"/>");
    }

    @Test
    void children() {
        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.attr("align", "left").child(HtmlChunk.br()).child(HtmlChunk.hr()))
            .hasToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.attr("align", "left").children(HtmlChunk.br()).children(HtmlChunk.hr()))
            .hasToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.attr("align", "left").children(List.of(HtmlChunk.br())).children(List.of(HtmlChunk.hr())))
            .hasToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.child(HtmlChunk.link("ref", "<foo>")))
            .hasToString("<p><a href=\"ref\">&lt;foo&gt;</a></p>");
    }

    @Test
    void wrapWith() {
        HtmlChunk foo = HtmlChunk.text("foo");
        assertThat(foo.wrapWith("p")).hasToString("<p>foo</p>");
        assertThat(foo.wrapWith(HtmlChunk.p())).hasToString("<p>foo</p>");
        assertThat(foo.bold()).hasToString("<b>foo</b>");
        assertThat(foo.code()).hasToString("<code>foo</code>");
        assertThat(foo.italic()).hasToString("<i>foo</i>");
        assertThat(foo.strikethrough()).hasToString("<s>foo</s>");
    }

//    @Test
//    void template() {
//        String userName = "Super<User>";
//        HtmlChunk greeting = HtmlChunk.template("Hello, $user$!", Map.entry("user", HtmlChunk.text(userName).wrapWith("b")));
//        assertThat(greeting).hasToString("Hello, <b>Super&lt;User&gt;</b>!");
//        HtmlChunk greeting2 = HtmlChunk.template("$user$$$$user$", Map.entry("user", HtmlChunk.text(userName).wrapWith("b")));
//        assertThat(greeting2).hasToString("<b>Super&lt;User&gt;</b>$<b>Super&lt;User&gt;</b>");
//    }

//    @Test
//    void icon() {
//        Icon icon = AllIcons.General.Gear;
//        assertNull(HtmlChunk.empty().findIcon("id"));
//        HtmlChunk chunk = HtmlChunk.icon("id", icon);
//        assertThat(chunk).hasToString("<icon src=\"id\"/>");
//        assertEquals(icon, chunk.findIcon("id"));
//        chunk = chunk.wrapWith("p");
//        assertEquals(icon, chunk.findIcon("id"));
//        assertThat(chunk).hasToString("<p><icon src=\"id\"/></p>");
//        chunk = HtmlChunk.fragment(HtmlChunk.text("Hello!"), chunk);
//        assertEquals(icon, chunk.findIcon("id"));
//        assertThat(chunk).hasToString("Hello!<p><icon src=\"id\"/></p>");
//    }

    @Test
    void toFragment() {
        assertThat(Stream.of("foo", "bar", "baz").map(t -> HtmlChunk.link(t, t)).collect(HtmlChunk.toFragment()))
            .hasToString("<a href=\"foo\">foo</a><a href=\"bar\">bar</a><a href=\"baz\">baz</a>");
        assertThat(Stream.of("foo", "bar", "baz").map(HtmlChunk::text).collect(HtmlChunk.toFragment(HtmlChunk.br())))
            .hasToString("foo<br/>bar<br/>baz");
        assertThat(HtmlChunk.fragment())
            .isSameAs(HtmlChunk.empty());
        assertThat(HtmlChunk.fragment(HtmlChunk.text("label:").wrapWith("span"), HtmlChunk.text("description")))
            .hasToString("<span>label:</span>description");
    }

    static LocalizeValue loc(@Nonnull String value) {
        return new TestLocalizedValue(value);
    }
}