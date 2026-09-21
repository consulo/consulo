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
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Point2D;
import consulo.ui.event.details.InputDetails;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code consulo.ui} components a tool window content builds itself from must be creatable headlessly, otherwise a
 * factory reaching for one fails on the UI thread and the content of that tool window never appears.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class HeadlessComponentsTest {
    @Test
    public void aButtonIsCreatableAndCarriesItsText() {
        Button button = Button.create(LocalizeValue.of("Send"));

        assertThat(button).isNotNull();
        assertThat(button.getText()).isEqualTo(LocalizeValue.of("Send"));

        button.setText(LocalizeValue.of("Stop"));
        assertThat(button.getText()).isEqualTo(LocalizeValue.of("Stop"));
        assertThat(button.getIcon()).isNull();
    }

    @Test
    public void invokingAButtonNotifiesItsClickListeners() {
        List<String> clicks = new ArrayList<>();
        Button button = Button.create(LocalizeValue.of("Clear"), event -> clicks.add("clicked"));

        button.invoke(new InputDetails(new Point2D(1, 2), new Point2D(3, 4)));

        assertThat(clicks).as("invoke must reach the listeners the way a rendered button does").containsExactly("clicked");
    }

    @Test
    public void aLayoutKeepsTheChildrenItIsGiven() {
        Component first = Label.create(LocalizeValue.of("first"));
        Component second = Label.create(LocalizeValue.of("second"));

        VerticalLayout layout = VerticalLayout.create();
        layout.add(first);
        layout.add(second);

        assertThat(children(layout)).containsExactly(first, second);

        layout.remove(first);
        assertThat(children(layout)).containsExactly(second);

        layout.removeAll();
        assertThat(children(layout)).isEmpty();
    }

    @Test
    public void aDockLayoutAcceptsItsPositionalChildren() {
        Component left = Label.create(LocalizeValue.of("left"));
        Component center = Label.create(LocalizeValue.of("center"));

        DockLayout layout = DockLayout.create().left(left).center(center);

        assertThat(children(layout)).containsExactly(left, center);
    }

    @Test
    public void aScrollableLayoutIsCreatableOverItsComponent() {
        Component scrolled = Label.create(LocalizeValue.of("conversation"));
        ScrollableLayout layout = ScrollableLayout.create(scrolled);

        assertThat(layout).isNotNull();
        assertThat(children(layout)).containsExactly(scrolled);
    }

    private static List<Component> children(Layout<?> layout) {
        List<Component> children = new ArrayList<>();
        layout.forEachChild(children::add);
        return children;
    }
}
