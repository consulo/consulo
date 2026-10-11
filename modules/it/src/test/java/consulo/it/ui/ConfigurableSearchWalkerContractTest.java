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

import consulo.configurable.internal.ConfigurableSearchHits;
import consulo.configurable.internal.ConfigurableSearchTextMatcher;
import consulo.configurable.internal.ConfigurableSearchWalker;
import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.Tab;
import consulo.ui.TextBox;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ConfigurableSearchWalkerContractTest {
    @Test
    public void everyKindOfCaptionIsMatched() {
        Label label = Label.create(LocalizeValue.of("Font"));
        CheckBox checkBox = CheckBox.create(LocalizeValue.of("Font"));
        RadioButton radioButton = RadioButton.create(LocalizeValue.of("Font"));
        Button button = Button.create(LocalizeValue.of("Font"));
        Hyperlink hyperlink = Hyperlink.create(LocalizeValue.of("Font"));
        Label other = Label.create(LocalizeValue.of("Colors"));
        VerticalLayout root = VerticalLayout.create()
            .add(label)
            .add(checkBox)
            .add(radioButton)
            .add(button)
            .add(hyperlink)
            .add(other);

        ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(root, matcher("font"));

        assertThat(hits.getTargets()).containsExactly(label, checkBox, radioButton, button, hyperlink);
        assertThat(hits.getTabTargets()).isEmpty();
        assertThat(hits.getTabsToSelect()).isEmpty();
    }

    @Test
    public void aLabelLightsTheFieldItIsFor() {
        TextBox field = TextBox.create();
        Label label = Label.create(LocalizeValue.of("Font"));
        label.setTarget(field);
        VerticalLayout root = VerticalLayout.create().add(label).add(field);

        ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(root, matcher("font"));

        assertThat(hits.getTargets()).containsExactly(label, field);
    }

    @Test
    public void theLoosePassRunsOnlyWhenTheStrictOneFindsNothing() {
        Label exact = Label.create(LocalizeValue.of("Font"));
        Label longer = Label.create(LocalizeValue.of("Editor font size"));
        VerticalLayout root = VerticalLayout.create().add(exact).add(longer);

        assertThat(ConfigurableSearchWalker.findHits(root, matcher("font")).getTargets()).containsExactly(exact);
        assertThat(ConfigurableSearchWalker.findHits(root, matcher("size")).getTargets()).containsExactly(longer);
        assertThat(ConfigurableSearchWalker.findHits(root, matcher("nothing")).isEmpty()).isTrue();
    }

    @Test
    public void mnemonicMarkersDoNotBreakAMatch() {
        Label label = Label.create(LocalizeValue.of("F&ont"));
        VerticalLayout root = VerticalLayout.create().add(label);

        assertThat(ConfigurableSearchWalker.findHits(root, matcher("font")).getTargets()).containsExactly(label);
    }

    @Test
    public void hiddenComponentsAreNotLit() {
        Label hidden = Label.create(LocalizeValue.of("Font"));
        hidden.setVisible(false);
        VerticalLayout root = VerticalLayout.create().add(hidden);

        assertThat(ConfigurableSearchWalker.findHits(root, matcher("font")).isEmpty()).isTrue();
    }

    @Test
    public void aSectionWhoseTitleMatchesIsLitAsAWholeWhenNothingInsideMatches() {
        LabeledLayout section = LabeledLayout.create(LocalizeValue.of("Font"), Label.create(LocalizeValue.of("Size")));
        FoldoutLayout foldout = FoldoutLayout.create(LocalizeValue.of("Font"), Label.create(LocalizeValue.of("Ligatures")));
        VerticalLayout root = VerticalLayout.create().add(section).add(foldout);

        assertThat(ConfigurableSearchWalker.findHits(root, matcher("font")).getTargets()).containsExactly(section, foldout);
    }

    @Test
    public void aSectionWithAHitInsideLightsTheHitOnly() {
        Label inner = Label.create(LocalizeValue.of("Font"));
        LabeledLayout section = LabeledLayout.create(LocalizeValue.of("Font"), inner);
        VerticalLayout root = VerticalLayout.create().add(section);

        assertThat(ConfigurableSearchWalker.findHits(root, matcher("font")).getTargets()).containsExactly(inner);
    }

    @Test
    public void aHitOnlyInAnotherTabSelectsThatTab() {
        Label general = Label.create(LocalizeValue.of("General"));
        Label font = Label.create(LocalizeValue.of("Font"));
        TabbedLayout tabs = TabbedLayout.create();
        Tab first = tabs.addTab(LocalizeValue.of("Main"), general);
        Tab second = tabs.addTab(LocalizeValue.of("Appearance"), font);

        assertThat(tabs.getSelectedTab()).isSameAs(first);

        ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(tabs, matcher("font"));

        assertThat(hits.getTabsToSelect()).containsExactly(second);
        assertThat(hits.getTargets()).containsExactly(font);
        assertThat(hits.getTabTargets()).isEmpty();
    }

    @Test
    public void aHitInTheSelectedTabKeepsTheSelectionAndPointsAtTheOtherTabs() {
        Label shownFont = Label.create(LocalizeValue.of("Font"));
        Label otherFont = Label.create(LocalizeValue.of("Font"));
        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(LocalizeValue.of("Main"), shownFont);
        Tab second = tabs.addTab(LocalizeValue.of("Appearance"), otherFont);

        ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(tabs, matcher("font"));

        assertThat(hits.getTabsToSelect()).isEmpty();
        assertThat(hits.getTargets()).containsExactly(shownFont);
        assertThat(hits.getTabTargets()).containsExactly(second);
    }

    @Test
    public void aMatchingTabTitleLightsItsCaption() {
        TabbedLayout tabs = TabbedLayout.create();
        Tab first = tabs.addTab(LocalizeValue.of("Font"), Label.create(LocalizeValue.of("Size")));
        tabs.addTab(LocalizeValue.of("Colors"), Label.create(LocalizeValue.of("Scheme")));

        ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(tabs, matcher("font"));

        assertThat(hits.getTabsToSelect()).isEmpty();
        assertThat(hits.getTabTargets()).containsExactly(first);
    }

    @Test
    public void selectingTheTabTheWalkerAsksForSwitchesTheLayout() {
        Label font = Label.create(LocalizeValue.of("Font"));
        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(LocalizeValue.of("Main"), Label.create(LocalizeValue.of("General")));
        Tab second = tabs.addTab(LocalizeValue.of("Appearance"), font);

        for (Tab tab : ConfigurableSearchWalker.findHits(tabs, matcher("font")).getTabsToSelect()) {
            tab.select();
        }

        assertThat(tabs.getSelectedTab()).isSameAs(second);
        assertThat(ConfigurableSearchWalker.findHits(tabs, matcher("font")).getTabsToSelect()).isEmpty();
    }

    @Test
    public void theTextsAreCollectedWithTheTabsTheyAreIn() {
        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(LocalizeValue.of("Appearance"), LabeledLayout.create(LocalizeValue.of("Fonts"), CheckBox.create(LocalizeValue.of("Ligatures"))));
        VerticalLayout root = VerticalLayout.create().add(Label.create(LocalizeValue.of("Header"))).add(tabs);

        List<String> collected = new ArrayList<>();
        ConfigurableSearchWalker.collectTexts(root, (text, tabPath) -> collected.add(
            tabPath.stream().map(LocalizeValue::get).reduce("", (path, title) -> path + title + "/") + text.get()
        ));

        assertThat(collected).containsExactly("Header", "Appearance", "Appearance/Fonts", "Appearance/Ligatures");
    }

    @Test
    public void aLocalizedValueIsMatchedByItsTextAndARawOneByItsRawText() {
        LocalizeValue localized = CommonLocalize.titleSettings();

        assertThat(ConfigurableSearchWalker.textsOf(localized)).contains(localized.get());
        assertThat(ConfigurableSearchWalker.textsOf(LocalizeValue.of("&Raw"))).containsExactly("Raw");
        assertThat(ConfigurableSearchWalker.textsOf(LocalizeValue.empty())).isEmpty();
    }

    private static ConfigurableSearchTextMatcher matcher(String query) {
        return (text, force) -> force ? text.equalsIgnoreCase(query) : text.toLowerCase().contains(query.toLowerCase());
    }
}
