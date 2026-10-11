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

import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.ui.HeadlessSpotlight;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Spotlight;
import consulo.ui.Tab;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SpotlightTest {
    @Test
    public void aSpotlightIsAttachedOverItsLayoutWithNothingLit() {
        VerticalLayout layout = VerticalLayout.create();

        Spotlight spotlight = Spotlight.over(layout);

        assertThat(spotlight).isInstanceOf(HeadlessSpotlight.class);
        assertThat(spotlight.getLayout()).isSameAs(layout);
        assertThat(spotlight.getTargets()).isEmpty();
        assertThat(headless(spotlight).isAttached()).isTrue();
        assertThat(headless(spotlight).getLitTargets()).isEmpty();
        assertThat(headless(spotlight).getUpdateCount()).isZero();
    }

    @Test
    public void theTargetsAreKeptInTheGivenOrderWithoutDuplicates() {
        Label first = Label.create(LocalizeValue.of("first"));
        CheckBox second = CheckBox.create(LocalizeValue.of("second"));
        VerticalLayout layout = VerticalLayout.create().add(first).add(second);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(second, first, second));

        assertThat(spotlight.getTargets()).containsExactly(second, first);
        assertThat(headless(spotlight).getLitTargets()).containsExactly(second, first);
    }

    @Test
    public void newTargetsReplaceThePreviousOnes() {
        Label first = Label.create(LocalizeValue.of("first"));
        Label second = Label.create(LocalizeValue.of("second"));
        VerticalLayout layout = VerticalLayout.create().add(first).add(second);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first));
        spotlight.setTargets(List.of(second));

        assertThat(spotlight.getTargets()).containsExactly(second);
        assertThat(headless(spotlight).getLitTargets()).containsExactly(second);
        assertThat(headless(spotlight).getUpdateCount()).isEqualTo(2);
    }

    @Test
    public void theSameTargetsAgainDoNotReachTheFrontend() {
        Label first = Label.create(LocalizeValue.of("first"));
        VerticalLayout layout = VerticalLayout.create().add(first);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first));
        spotlight.setTargets(List.of(first));

        assertThat(headless(spotlight).getUpdateCount()).isEqualTo(1);
    }

    @Test
    public void theTargetsAreACopyOfWhatWasGiven() {
        Label first = Label.create(LocalizeValue.of("first"));
        Label second = Label.create(LocalizeValue.of("second"));
        VerticalLayout layout = VerticalLayout.create().add(first).add(second);

        List<Component> targets = new ArrayList<>(List.of(first));

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(targets);
        targets.add(second);

        assertThat(spotlight.getTargets()).containsExactly(first);
    }

    @Test
    public void clearTakesTheDimAway() {
        Label first = Label.create(LocalizeValue.of("first"));
        VerticalLayout layout = VerticalLayout.create().add(first);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first));
        spotlight.clear();

        assertThat(spotlight.getTargets()).isEmpty();
        assertThat(headless(spotlight).getLitTargets()).isEmpty();
        assertThat(headless(spotlight).isAttached()).isTrue();
    }

    @Test
    public void aDisposedSpotlightIgnoresNewTargets() {
        Label first = Label.create(LocalizeValue.of("first"));
        VerticalLayout layout = VerticalLayout.create().add(first);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first));
        spotlight.dispose();

        assertThat(headless(spotlight).isDisposed()).isTrue();
        assertThat(headless(spotlight).isAttached()).isFalse();
        assertThat(spotlight.getTargets()).isEmpty();

        spotlight.setTargets(List.of(first));

        assertThat(spotlight.getTargets()).isEmpty();
        assertThat(headless(spotlight).getLitTargets()).isEmpty();
        assertThat(headless(spotlight).getUpdateCount()).isEqualTo(1);
    }

    @Test
    public void disposingTwiceIsHarmless() {
        Spotlight spotlight = Spotlight.over(VerticalLayout.create());

        Disposer.dispose(spotlight);
        spotlight.dispose();

        assertThat(headless(spotlight).isDisposed()).isTrue();
    }

    @Test
    public void aSpotlightWithoutAnOwnerLivesUntilItIsDisposed() {
        Label first = Label.create(LocalizeValue.of("first"));
        VerticalLayout layout = VerticalLayout.create().add(first);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first));

        assertThat(headless(spotlight).isDisposed()).isFalse();
        assertThat(spotlight.getTargets()).containsExactly(first);
    }

    @Test
    public void tabCaptionsAreLitAlongWithTheTargets() {
        Label first = Label.create(LocalizeValue.of("first"));
        TabbedLayout tabs = TabbedLayout.create();
        Tab tab = tabs.addTab(LocalizeValue.of("tab"), first);
        VerticalLayout layout = VerticalLayout.create().add(tabs);

        Spotlight spotlight = Spotlight.over(layout);
        spotlight.setTargets(List.of(first), List.of(tab, tab));

        assertThat(spotlight.getTargets()).containsExactly(first);
        assertThat(spotlight.getTabTargets()).containsExactly(tab);
        assertThat(headless(spotlight).getLitTabs()).containsExactly(tab);
    }

    @Test
    public void aTabCaptionAloneDims() {
        TabbedLayout tabs = TabbedLayout.create();
        Tab tab = tabs.addTab(LocalizeValue.of("tab"), Label.create(LocalizeValue.of("first")));

        Spotlight spotlight = Spotlight.over(VerticalLayout.create().add(tabs));
        spotlight.setTargets(List.of(), List.of(tab));

        assertThat(headless(spotlight).isLit()).isTrue();
        assertThat(headless(spotlight).getUpdateCount()).isEqualTo(1);

        spotlight.clear();

        assertThat(headless(spotlight).isLit()).isFalse();
        assertThat(spotlight.getTabTargets()).isEmpty();
        assertThat(headless(spotlight).getLitTabs()).isEmpty();
    }

    @Test
    public void targetsWithoutTabsDropThePreviousTabs() {
        Label first = Label.create(LocalizeValue.of("first"));
        TabbedLayout tabs = TabbedLayout.create();
        Tab tab = tabs.addTab(LocalizeValue.of("tab"), first);

        Spotlight spotlight = Spotlight.over(VerticalLayout.create().add(tabs));
        spotlight.setTargets(List.of(first), List.of(tab));
        spotlight.setTargets(List.of(first));

        assertThat(spotlight.getTabTargets()).isEmpty();
        assertThat(headless(spotlight).getUpdateCount()).isEqualTo(2);
    }

    private static HeadlessSpotlight headless(Spotlight spotlight) {
        return (HeadlessSpotlight) spotlight;
    }
}
