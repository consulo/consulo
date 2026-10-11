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
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TabbedLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The captions of tabs and sections are readable as values, so a search can match them in the current locale and in
 * the default one, without anything being drawn.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class LayoutTitlesContractTest {
    @Test
    public void aTabAddedByNameIsTitledByThatName() {
        TabbedLayout layout = TabbedLayout.create();

        Tab tab = layout.addTab("General", label("content"));

        assertThat(tab.getTitle().get()).isEqualTo("General");
    }

    @Test
    public void aTabAddedByALocalizedNameAnswersThatValueWithItsKey() {
        LocalizeValue name = CommonLocalize.titleSettings();
        TabbedLayout layout = TabbedLayout.create();

        Tab tab = layout.addTab(name, label("content"));

        assertThat(tab.getTitle()).isSameAs(name);
        assertThat(tab.getTitle().getKey()).isPresent();
    }

    @Test
    public void aRendererOfSeveralRunsTitlesTheTabWithTheRunsJoined() {
        TabbedLayout layout = TabbedLayout.create();
        Tab tab = layout.createTab();
        tab.setRenderer((t, presentation) -> {
            presentation.append(LocalizeValue.of("Code "));
            presentation.append(LocalizeValue.of("Style"), TextAttribute.REGULAR_BOLD);
            presentation.withSuffix(LocalizeValue.of("suffix"), null);
        });

        assertThat(tab.getTitle().get()).isEqualTo("Code Style");
    }

    @Test
    public void aRendererWhichAppendsNothingTitlesTheTabEmpty() {
        TabbedLayout layout = TabbedLayout.create();
        Tab tab = layout.createTab();
        tab.setRenderer((t, presentation) -> {
        });

        assertThat(tab.getTitle()).isEqualTo(LocalizeValue.empty());
    }

    @Test
    public void theTitleIsReadOffTheRendererWithoutTheTabBeingShown() {
        TabbedLayout layout = TabbedLayout.create();
        Tab tab = layout.createTab();
        List<String> calls = new ArrayList<>();
        tab.setRenderer((t, presentation) -> {
            calls.add("render");
            presentation.append("Never shown");
        });

        assertThat(tab.getTitle().get()).isEqualTo("Never shown");
        assertThat(calls).containsExactly("render");
    }

    @Test
    public void forEachTabPairsEveryTabWithItsContentInTabOrder() {
        Label general = label("general");
        Label advanced = label("advanced");

        TabbedLayout layout = TabbedLayout.create();
        Tab generalTab = layout.addTab(LocalizeValue.of("General"), general);
        Tab advancedTab = layout.addTab(LocalizeValue.of("Advanced"), advanced);

        List<Map.Entry<Tab, Component>> tabs = new ArrayList<>();
        layout.forEachTab((tab, content) -> tabs.add(Map.entry(tab, content)));

        assertThat(tabs).containsExactly(Map.entry(generalTab, general), Map.entry(advancedTab, advanced));
        assertThat(tabs).extracting(entry -> entry.getKey().getTitle().get()).containsExactly("General", "Advanced");
    }

    @Test
    public void aLabeledLayoutAnswersItsCaption() {
        LocalizeValue caption = CommonLocalize.titleSettings();

        LabeledLayout layout = LabeledLayout.create(caption, label("content"));

        assertThat(layout.getLabel()).isSameAs(caption);
    }

    @Test
    public void aFoldoutLayoutAnswersItsCaptionAsLastSet() {
        FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Advanced"), label("content"));
        assertThat(layout.getTitle().get()).isEqualTo("Advanced");

        LocalizeValue renamed = CommonLocalize.titleSettings();
        layout.setTitle(renamed);
        assertThat(layout.getTitle()).isSameAs(renamed);
    }

    private static Label label(String text) {
        return Label.create(LocalizeValue.of(text));
    }
}
