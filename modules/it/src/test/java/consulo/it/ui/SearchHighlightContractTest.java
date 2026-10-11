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
package consulo.it.ui;

import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.HasSearchHighlight;
import consulo.ui.HtmlLabel;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.ToggleButton;
import consulo.ui.internal.SearchHighlightUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The search highlight is a state of the text components, kept across text changes and cleared by {@code null}, and
 * the runs it marks follow one rule on every frontend.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SearchHighlightContractTest {
    @Test
    public void everyTextComponentKeepsTheQueryUntilItIsCleared() {
        for (HasSearchHighlight component : textComponents()) {
            assertThat(component.getSearchHighlight()).as(component.getClass().getSimpleName()).isNull();

            component.setSearchHighlight("tab size");
            assertThat(component.getSearchHighlight()).as(component.getClass().getSimpleName()).isEqualTo("tab size");

            component.setSearchHighlight(null);
            assertThat(component.getSearchHighlight()).as(component.getClass().getSimpleName()).isNull();
        }
    }

    @Test
    public void aQueryWithoutWordsClearsTheHighlight() {
        for (HasSearchHighlight component : textComponents()) {
            component.setSearchHighlight("tab");
            component.setSearchHighlight("  \t ");

            assertThat(component.getSearchHighlight()).as(component.getClass().getSimpleName()).isNull();
        }
    }

    @Test
    public void theHighlightStaysWhenTheTextChanges() {
        Label label = Label.create(LocalizeValue.of("Tab size"));
        label.setSearchHighlight("size");

        label.setText(LocalizeValue.of("Indent size"));

        assertThat(label.getSearchHighlight()).isEqualTo("size");
    }

    @Test
    public void everyWordOfTheQueryIsMatchedWithoutRegardToCase() {
        assertThat(fragments("Tab size", "SIZE ta")).isEqualTo("[Ta]b [size]");
    }

    @Test
    public void aWordMatchesInsideALongerWord() {
        assertThat(fragments("Resize the tab", "size")).isEqualTo("Re[size] the tab");
    }

    @Test
    public void everyOccurrenceIsMatched() {
        assertThat(fragments("tab, tab, tab", "tab")).isEqualTo("[tab], [tab], [tab]");
    }

    @Test
    public void matchesWhichOverlapOrTouchAreOneRun() {
        assertThat(fragments("abcdef", "abc cd def")).isEqualTo("[abcdef]");
    }

    @Test
    public void aTextTheQueryDoesNotMatchIsOneUnmarkedRun() {
        assertThat(SearchHighlightUtil.matches("Tab size", "indent")).isFalse();
        assertThat(fragments("Tab size", "indent")).isEqualTo("Tab size");
        assertThat(fragments("Tab size", null)).isEqualTo("Tab size");
    }

    @Test
    public void aPlainTextIsEscapedWhenItIsMarkedUp() {
        assertThat(SearchHighlightUtil.markupText("a < b & tab", "tab", "[", "]")).isEqualTo("a &lt; b &amp; [tab]");
    }

    @Test
    public void markupKeepsItsTagsAndHasOnlyItsShownTextMarked() {
        String html = "<html><body><a href=\"tab\">Tab</a> size</body></html>";

        assertThat(SearchHighlightUtil.markupHtml(html, "tab", "[", "]"))
            .isEqualTo("<html><body><a href=\"tab\">[Tab]</a> size</body></html>");
    }

    @Test
    public void aMatchAcrossATagIsMarkedOnEachSideOfIt() {
        assertThat(SearchHighlightUtil.markupHtml("<b>Ta</b>b", "tab", "[", "]")).isEqualTo("<b>[Ta]</b>[b]");
    }

    @Test
    public void anEntityIsMatchedByWhatItShowsAndNeverSplit() {
        assertThat(SearchHighlightUtil.markupHtml("Fish &amp; chips", "&", "[", "]")).isEqualTo("Fish [&amp;] chips");
        assertThat(SearchHighlightUtil.markupHtml("Fish &amp; chips", "amp", "[", "]")).isEqualTo("Fish &amp; chips");
    }

    @Test
    public void theContentOfAStyleElementIsNotShownTextAndIsNotMatched() {
        String html = "<html><head><style>.tab { color: red }</style></head><body>tab</body></html>";

        assertThat(SearchHighlightUtil.markupHtml(html, "tab", "[", "]"))
            .isEqualTo("<html><head><style>.tab { color: red }</style></head><body>[tab]</body></html>");
    }

    private static List<HasSearchHighlight> textComponents() {
        return List.of(
            Label.create(LocalizeValue.of("Tab size")),
            HtmlLabel.create(LocalizeValue.of("<b>Tab</b> size")),
            CheckBox.create(LocalizeValue.of("Use &tab character")),
            RadioButton.create(LocalizeValue.of("Tab size")),
            Button.create(LocalizeValue.of("Tab size")),
            ToggleButton.create(LocalizeValue.of("Tab size")),
            Hyperlink.create(LocalizeValue.of("Tab size"))
        );
    }

    private static String fragments(String text, @Nullable String query) {
        StringBuilder builder = new StringBuilder();
        SearchHighlightUtil.forEachFragment(text, query, (fragment, matched) -> {
            builder.append(matched ? "[" + fragment + "]" : fragment);
        });
        return builder.toString();
    }
}
