// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.localize.LocalizeValue;
import consulo.localize.Localized;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-02-18
 */
@SuppressWarnings("deprecation")
public class HtmlChunkTest {
    @Test
    void text() {
        assertThat(HtmlChunk.text("foo")).hasIdAndToString("foo");
        assertThat(HtmlChunk.text("<a href=\"hello\">")).hasIdAndToString("&lt;a href=\"hello\"&gt;");

        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.addText(loc("<foo>"))).hasToString("<p>&lt;foo&gt;</p>").hasId("<p>TestLocalizedValue{&lt;foo&gt;}</p>");
        assertThat(p.addText("<foo>")).hasIdAndToString("<p>&lt;foo&gt;</p>");
    }

    @Test
    void raw() {
        assertThat(HtmlChunk.raw("foo")).hasIdAndToString("foo");
        assertThat(HtmlChunk.raw("<a href=\"hello\">")).hasIdAndToString("<a href=\"hello\">");

        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.addRaw(loc("<foo>"))).hasToString("<p><foo></p>").hasId("<p>TestLocalizedValue{<foo>}</p>");
        assertThat(p.addRaw("<foo>")).hasIdAndToString("<p><foo></p>");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void htmlEntity() {
        assertThat(HtmlChunk.htmlEntity("&foo;")).hasIdAndToString("&foo;");
        assertThatThrownBy(() -> HtmlChunk.htmlEntity("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Not an entity: foo");
    }

    @Test
    void nbsp() {
        assertThat(HtmlChunk.nbsp()).hasIdAndToString("&nbsp;");
        assertThat(HtmlChunk.nbsp(3)).hasIdAndToString("&nbsp;&nbsp;&nbsp;");
        assertThatThrownBy(() -> new HtmlBuilder().nbsp(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("0 is not positive");
        assertThatThrownBy(() -> new HtmlBuilder().nbsp(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("-1 is not positive");
    }

    @Test
    void tag() {
        assertThat(HtmlChunk.tag("b")).hasIdAndToString("<b/>");
    }

    @Test
    void body() {
        assertThat(HtmlChunk.body()).hasIdAndToString("<body/>");
    }

    @Test
    void br() {
        assertThat(HtmlChunk.br()).hasIdAndToString("<br/>");
    }

    @Test
    void div() {
        assertThat(HtmlChunk.div()).hasIdAndToString("<div/>");
        assertThat(HtmlChunk.div("color: blue")).hasIdAndToString("<div style=\"color: blue\"/>");
    }

    @Test
    void font() {
        assertThat(HtmlChunk.font(12)).hasIdAndToString("<font size=\"12\"/>");
        assertThat(HtmlChunk.font("blue")).hasIdAndToString("<font color=\"blue\"/>");
    }

    @Test
    void hr() {
        assertThat(HtmlChunk.hr()).hasIdAndToString("<hr/>");
    }

    @Test
    void head() {
        assertThat(HtmlChunk.head()).hasIdAndToString("<head/>");
    }

    @Test
    void html() {
        assertThat(HtmlChunk.html()).hasIdAndToString("<html/>");
    }

    @Test
    void li() {
        assertThat(HtmlChunk.li()).hasIdAndToString("<li/>");
    }

    @Test
    void link() {
        assertThat(HtmlChunk.link("target", loc("<Click & me>")))
            .hasToString("<a href=\"target\">&lt;Click &amp; me&gt;</a>")
            .hasId("<a href=\"target\">TestLocalizedValue{&lt;Click &amp; me&gt;}</a>");
        assertThat(HtmlChunk.link("target", "<Click & me>"))
            .hasIdAndToString("<a href=\"target\">&lt;Click &amp; me&gt;</a>");
        assertThat(HtmlChunk.link("target", HtmlChunk.text("<Click & me>")))
            .hasIdAndToString("<a href=\"target\">&lt;Click &amp; me&gt;</a>");
    }

    @Test
    void p() {
        assertThat(HtmlChunk.p()).hasIdAndToString("<p/>");
    }

    @Test
    void span() {
        assertThat(HtmlChunk.span()).hasIdAndToString("<span/>");
        assertThat(HtmlChunk.span("color: blue")).hasIdAndToString("<span style=\"color: blue\"/>");
    }

    @Test
    void styleTag() {
        assertThat(HtmlChunk.styleTag("* {\ncolor: blue;\n}\n")).hasIdAndToString("<style>* {\ncolor: blue;\n}\n</style>");
    }

    @Test
    void ul() {
        assertThat(HtmlChunk.ul()).hasIdAndToString("<ul/>");
    }

    @Test
    void attr() {
        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.attr("align", "left")).hasIdAndToString("<p align=\"left\"/>");
        assertThat(p.attr("align", "left").attr("align", "right")).hasIdAndToString("<p align=\"right\"/>");
        assertThat(HtmlChunk.tag("img").attr("width", 16).attr("height", 16)).hasIdAndToString("<img height=\"16\" width=\"16\"/>");
        assertThat(HtmlChunk.tag("input").attr("disabled")).hasIdAndToString("<input disabled/>");
        assertThat(p.style("color: blue")).hasIdAndToString("<p style=\"color: blue\"/>");
        assertThat(p.setClass("invisible")).hasIdAndToString("<p class=\"invisible\"/>");
    }

    @Test
    void children() {
        HtmlChunk.Element p = HtmlChunk.p();
        assertThat(p.attr("align", "left").child(HtmlChunk.br()).child(HtmlChunk.hr()))
            .hasIdAndToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.attr("align", "left").children(HtmlChunk.br()).children(HtmlChunk.hr()))
            .hasIdAndToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.attr("align", "left").children(List.of(HtmlChunk.br())).children(List.of(HtmlChunk.hr())))
            .hasIdAndToString("<p align=\"left\"><br/><hr/></p>");
        assertThat(p.child(HtmlChunk.link("ref", "<foo>")))
            .hasIdAndToString("<p><a href=\"ref\">&lt;foo&gt;</a></p>");
    }

    @Test
    void wrapWith() {
        HtmlChunk foo = HtmlChunk.text("foo");
        assertThat(foo.wrapWith("p")).hasIdAndToString("<p>foo</p>");
        assertThat(foo.wrapWith(HtmlChunk.p())).hasIdAndToString("<p>foo</p>");
        assertThat(foo.bold()).hasIdAndToString("<b>foo</b>");
        assertThat(foo.code()).hasIdAndToString("<code>foo</code>");
        assertThat(foo.italic()).hasIdAndToString("<i>foo</i>");
        assertThat(foo.strikethrough()).hasIdAndToString("<s>foo</s>");
    }

//    @Test
//    void template() {
//        String userName = "Super<User>";
//        HtmlChunk greeting = HtmlChunk.template("Hello, $user$!", Map.entry("user", HtmlChunk.text(userName).wrapWith("b")));
//        assertThat(greeting).hasIdAndToString("Hello, <b>Super&lt;User&gt;</b>!");
//        HtmlChunk greeting2 = HtmlChunk.template("$user$$$$user$", Map.entry("user", HtmlChunk.text(userName).wrapWith("b")));
//        assertThat(greeting2).hasIdAndToString("<b>Super&lt;User&gt;</b>$<b>Super&lt;User&gt;</b>");
//    }

//    @Test
//    void icon() {
//        Icon icon = AllIcons.General.Gear;
//        assertNull(HtmlChunk.empty().findIcon("id"));
//        HtmlChunk chunk = HtmlChunk.icon("id", icon);
//        assertThat(chunk).hasIdAndToString("<icon src=\"id\"/>");
//        assertEquals(icon, chunk.findIcon("id"));
//        chunk = chunk.wrapWith("p");
//        assertEquals(icon, chunk.findIcon("id"));
//        assertThat(chunk).hasIdAndToString("<p><icon src=\"id\"/></p>");
//        chunk = HtmlChunk.fragment(HtmlChunk.text("Hello!"), chunk);
//        assertEquals(icon, chunk.findIcon("id"));
//        assertThat(chunk).hasIdAndToString("Hello!<p><icon src=\"id\"/></p>");
//    }

    @Test
    void toFragment() {
        assertThat(Stream.of("foo", "bar", "baz").map(t -> HtmlChunk.link(t, t)).collect(HtmlChunk.toFragment()))
            .hasIdAndToString("<a href=\"foo\">foo</a><a href=\"bar\">bar</a><a href=\"baz\">baz</a>");
        assertThat(Stream.of("foo", "bar", "baz").map(HtmlChunk::text).collect(HtmlChunk.toFragment(HtmlChunk.br())))
            .hasIdAndToString("foo<br/>bar<br/>baz");
        assertThat(HtmlChunk.fragment())
            .isSameAs(HtmlChunk.empty());
        assertThat(HtmlChunk.fragment(HtmlChunk.text("label:").wrapWith("span"), HtmlChunk.text("description")))
            .hasIdAndToString("<span>label:</span>description");
    }

    static LocalizeValue loc(String value) {
        return new TestLocalizedValue(value);
    }

    static <T extends Localized> LocalizedAssert<T> assertThat(T actual) {
        return new LocalizedAssert<>(actual);
    }
}
