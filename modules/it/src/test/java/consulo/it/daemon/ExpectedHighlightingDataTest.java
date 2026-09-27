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
package consulo.it.daemon;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * The markup parser and matcher are plain string work, so they are exercised without a project.
 *
 * @author VISTALL
 */
public class ExpectedHighlightingDataTest {
    @Test
    public void tagIsStrippedAndOffsetsPointIntoTheStrippedText() {
        ExpectedHighlightingData data = ExpectedHighlightingData.parse("class <error descr=\"boom\">Item</error> {}");

        assertThat(data.getText()).isEqualTo("class Item {}");
        assertThat(data.getExpected()).hasSize(1);

        ExpectedHighlight expected = data.getExpected().get(0);
        assertThat(expected.getStartOffset()).isEqualTo(6);
        assertThat(expected.getEndOffset()).isEqualTo(10);
        assertThat(expected.getDescription()).isEqualTo("boom");
        assertThat(data.getText().substring(expected.getStartOffset(), expected.getEndOffset()))
            .as("offsets address the stripped text, not the markup")
            .isEqualTo("Item");
    }

    @Test
    public void everyTagMapsToItsSeverity() {
        assertThat(severityOf("error")).isEqualTo(HighlightSeverity.ERROR);
        assertThat(severityOf("warning")).isEqualTo(HighlightSeverity.WARNING);
        assertThat(severityOf("weak_warning")).isEqualTo(HighlightSeverity.WEAK_WARNING);
        assertThat(severityOf("info")).isEqualTo(HighlightSeverity.INFORMATION);
        assertThat(severityOf("text_attr")).isEqualTo(HighlightSeverity.TEXT_ATTRIBUTES);
        assertThat(severityOf("EOLError")).isEqualTo(HighlightSeverity.ERROR);
        assertThat(severityOf("EOLWarning")).isEqualTo(HighlightSeverity.WARNING);
    }

    @Test
    public void eolTagsOnlyMatchHighlightsPlacedAfterTheLineEnd() {
        ExpectedHighlight eol = only("a<EOLError descr=\"x\"></EOLError>");
        ExpectedHighlight inline = only("a<error descr=\"x\"></error>");

        assertThat(eol.matches(FakeHighlightInfo.afterEndOfLine(HighlightSeverity.ERROR, 1, 1, "x"))).isTrue();
        assertThat(eol.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 1, 1, "x"))).isFalse();
        assertThat(inline.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 1, 1, "x"))).isTrue();
        assertThat(inline.matches(FakeHighlightInfo.afterEndOfLine(HighlightSeverity.ERROR, 1, 1, "x"))).isFalse();
    }

    @Test
    public void tagsNest() {
        ExpectedHighlightingData data =
            ExpectedHighlightingData.parse("<warning descr=\"outer\">a<error descr=\"inner\">b</error>c</warning>");

        assertThat(data.getText()).isEqualTo("abc");
        assertThat(data.getExpected()).hasSize(2);
        assertThat(data.byTag().keySet()).containsExactlyInAnyOrder("warning", "error");

        ExpectedHighlight inner = data.byTag().get("error").get(0);
        ExpectedHighlight outer = data.byTag().get("warning").get(0);
        assertThat(inner.getStartOffset()).isEqualTo(1);
        assertThat(inner.getEndOffset()).isEqualTo(2);
        assertThat(outer.getStartOffset()).isEqualTo(0);
        assertThat(outer.getEndOffset()).isEqualTo(3);
    }

    @Test
    public void missingDescriptionMatchesAnything() {
        ExpectedHighlight expected = only("<error>x</error>");

        assertThat(expected.getDescription()).isEqualTo(ExpectedHighlight.ANY_TEXT);
        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 1, "whatever"))).isTrue();
        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 1, null))).isTrue();
    }

    @Test
    public void descriptionEndingInAnEllipsisMatchesByPrefix() {
        ExpectedHighlight shortened = only("<error descr=\"cannot resolve...\">x</error>");

        assertThat(shortened.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 1, "cannot resolve symbol 'y'"))).isTrue();
        assertThat(shortened.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 1, "could not resolve"))).isFalse();
    }

    @Test
    public void severityAndOffsetsHaveToAgree() {
        ExpectedHighlight expected = only("<error descr=\"x\">ab</error>");

        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 2, "x"))).isTrue();
        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.WARNING, 0, 2, "x"))).isFalse();
        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 1, "x"))).isFalse();
        assertThat(expected.matches(FakeHighlightInfo.of(HighlightSeverity.ERROR, 1, 2, "x"))).isFalse();
    }

    @Test
    public void escapedQuotesSurviveInTheDescription() {
        assertThat(only("<error descr=\"say \\\"hi\\\"\">x</error>").getDescription()).isEqualTo("say \"hi\"");
    }

    @Test
    public void textThatMerelyLooksLikeMarkupIsLeftAlone() {
        assertThat(ExpectedHighlightingData.parse("a < b && c <foo> d").getText()).isEqualTo("a < b && c <foo> d");
        assertThat(ExpectedHighlightingData.parse("a < b").getExpected()).isEmpty();
    }

    @Test
    public void malformedMarkupIsRejected() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("<error descr=\"x\">unclosed"))
            .withMessageContaining("never closed");

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("stray</error>"))
            .withMessageContaining("never opened");

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("<error type=\"WRONG_REF\">x</error>"))
            .withMessageContaining("does not support the attribute 'type'");
    }

    @Test
    public void checkResultReportsBothDirections() {
        ExpectedHighlightingData data = ExpectedHighlightingData.parse("<error descr=\"wanted\">ab</error>");

        assertThatNoException().isThrownBy(() ->
            data.checkResult(List.<HighlightInfo>of(FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 2, "wanted"))));

        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> data.checkResult(List.of()))
            .withMessageContaining("expected but not produced");

        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> data.checkResult(List.<HighlightInfo>of(
                FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 2, "wanted"),
                FakeHighlightInfo.of(HighlightSeverity.ERROR, 5, 6, "surprise")
            )))
            .withMessageContaining("produced but not expected");
    }

    @Test
    public void onlySeveritiesTheMarkupMentionsTakePart() {
        ExpectedHighlightingData errorsOnly = ExpectedHighlightingData.parse("<error descr=\"wanted\">ab</error>");

        assertThatNoException().isThrownBy(() ->
            errorsOnly.checkResult(List.<HighlightInfo>of(
                FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 2, "wanted"),
                FakeHighlightInfo.of(HighlightSeverity.WARNING, 5, 6, "a warning nobody asked about"),
                FakeHighlightInfo.of(HighlightSeverity.INFORMATION, 7, 8, "an info nobody asked about")
            )));

        ExpectedHighlightingData alsoWarnings =
            ExpectedHighlightingData.parse("<error descr=\"wanted\">ab</error> <warning descr=\"w\">c</warning>");

        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> alsoWarnings.checkResult(List.<HighlightInfo>of(
                FakeHighlightInfo.of(HighlightSeverity.ERROR, 0, 2, "wanted"),
                FakeHighlightInfo.of(HighlightSeverity.WARNING, 3, 4, "w"),
                FakeHighlightInfo.of(HighlightSeverity.WARNING, 5, 6, "an extra warning")
            )))
            .withMessageContaining("produced but not expected");
    }

    @Test
    public void lineMarkersAreCollectedSeparatelyFromHighlights() {
        ExpectedHighlightingData data = ExpectedHighlightingData.parse(
            "<lineMarker descr=\"gutter\">class</lineMarker> <error descr=\"boom\">Item</error> {}");

        assertThat(data.getText()).isEqualTo("class Item {}");
        assertThat(data.getExpected()).hasSize(1);
        assertThat(data.getExpectedLineMarkers()).hasSize(1);

        ExpectedLineMarker marker = data.getExpectedLineMarkers().get(0);
        assertThat(marker.getStartOffset()).isEqualTo(0);
        assertThat(marker.getEndOffset()).isEqualTo(5);
        assertThat(marker.getDescription()).isEqualTo("gutter");
        assertThat(data.getText().substring(marker.getStartOffset(), marker.getEndOffset())).isEqualTo("class");
    }

    @Test
    public void lineMarkerNestsWithHighlights() {
        ExpectedHighlightingData data = ExpectedHighlightingData.parse(
            "<lineMarker descr=\"g\">a<error descr=\"e\">b</error>c</lineMarker>");

        assertThat(data.getText()).isEqualTo("abc");
        assertThat(data.getExpected()).hasSize(1);
        assertThat(data.getExpectedLineMarkers()).hasSize(1);
        assertThat(data.getExpectedLineMarkers().get(0).getEndOffset()).isEqualTo(3);
        assertThat(data.getExpected().get(0).getStartOffset()).isEqualTo(1);
    }

    @Test
    public void lineMarkerIconIsRejectedBecauseThereIsNothingToCompare() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("<lineMarker icon=\"nodes/class.svg\">a</lineMarker>"))
            .withMessageContaining("does not support the attribute 'icon'");
    }

    @Test
    public void lineMarkerWithoutDescriptionMatchesAnyTooltip() {
        assertThat(ExpectedHighlightingData.parse("<lineMarker>a</lineMarker>").getExpectedLineMarkers().get(0).getDescription())
            .isEqualTo(ExpectedHighlight.ANY_TEXT);
    }

    @Test
    public void caretMarkerIsStrippedAndItsOffsetReported() {
        ExpectedHighlightingData data = ExpectedHighlightingData.parse("class It<caret>em {}");

        assertThat(data.getText()).isEqualTo("class Item {}");
        assertThat(data.getCaretOffset()).isEqualTo(8);
        assertThat(data.getText().charAt(data.getCaretOffset())).isEqualTo('e');
    }

    @Test
    public void caretAndHighlightTagsAgreeOnOffsets() {
        ExpectedHighlightingData data =
            ExpectedHighlightingData.parse("class <error descr=\"boom\">It<caret>em</error> {}");

        assertThat(data.getText()).isEqualTo("class Item {}");
        assertThat(data.getCaretOffset()).isEqualTo(8);
        assertThat(data.getExpected().get(0).getStartOffset()).isEqualTo(6);
        assertThat(data.getExpected().get(0).getEndOffset()).isEqualTo(10);
    }

    @Test
    public void askingForOneCaretWhenThereIsNotExactlyOneFails() {
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("no caret here").getCaretOffset())
            .withMessageContaining("found 0");

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> ExpectedHighlightingData.parse("a<caret>b<caret>c").getCaretOffset())
            .withMessageContaining("found 2");

        assertThat(ExpectedHighlightingData.parse("a<caret>b<caret>c").getCaretOffsets()).containsExactly(1, 2);
    }

    private static HighlightSeverity severityOf(String tag) {
        return only("<" + tag + " descr=\"x\">a</" + tag + ">").getSeverity();
    }

    private static ExpectedHighlight only(String markup) {
        List<ExpectedHighlight> expected = ExpectedHighlightingData.parse(markup).getExpected();
        assertThat(expected).hasSize(1);
        return expected.get(0);
    }
}
