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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.layout.WrappedLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every layout tells what it holds through {@link Layout#forEachChild}, in the order it shows it - the walk which
 * indexes a settings page for search depends on it.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class LayoutForEachChildContractTest {
    @Test
    public void aVerticalLayoutVisitsItsRowsTopDown() {
        Label first = label("first");
        Label second = label("second");

        VerticalLayout layout = VerticalLayout.create();
        layout.add(first);
        layout.add(second);

        assertThat(children(layout)).containsExactly(first, second);
    }

    @Test
    public void aHorizontalLayoutVisitsItsColumnsFromTheLeadingSide() {
        Label first = label("first");
        Label second = label("second");

        HorizontalLayout layout = HorizontalLayout.create();
        layout.add(first);
        layout.add(second);

        assertThat(children(layout)).containsExactly(first, second);
    }

    @Test
    public void aDockLayoutVisitsItsSidesInReadingOrderWhateverOrderTheyWereAddedIn() {
        Label top = label("top");
        Label left = label("left");
        Label center = label("center");
        Label right = label("right");
        Label bottom = label("bottom");

        DockLayout layout = DockLayout.create().bottom(bottom).right(right).center(center).left(left).top(top);

        assertThat(children(layout)).containsExactly(top, left, center, right, bottom);
    }

    @Test
    public void aDockLayoutVisitsOnlyWhatIsLeftInASideWhichWasFilledTwice() {
        Label replaced = label("replaced");
        Label center = label("center");

        DockLayout layout = DockLayout.create().center(replaced).center(center);

        assertThat(children(layout)).containsExactly(center);
    }

    @Test
    public void aTableLayoutVisitsItsCellsRowByRow() {
        Label secondRowFirstColumn = label("1:0");
        Label firstRowSecondColumn = label("0:1");
        Label firstRowFirstColumn = label("0:0");

        TableLayout layout = TableLayout.create(StaticPosition.TOP);
        layout.add(secondRowFirstColumn, TableLayout.cell(1, 0));
        layout.add(firstRowSecondColumn, TableLayout.cell(0, 1));
        layout.add(firstRowFirstColumn, TableLayout.cell(0, 0));

        assertThat(children(layout)).containsExactly(firstRowFirstColumn, firstRowSecondColumn, secondRowFirstColumn);
    }

    @Test
    public void aLabeledLayoutVisitsItsContent() {
        Label content = label("content");

        LabeledLayout layout = LabeledLayout.create(LocalizeValue.of("Section"), content);

        assertThat(children(layout)).containsExactly(content);
    }

    @Test
    public void aFoldoutLayoutVisitsItsContentEvenWhileFolded() {
        Label content = label("content");

        FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Advanced"), content, false);

        assertThat(children(layout)).containsExactly(content);
    }

    @Test
    public void aScrollableLayoutVisitsWhatItScrolls() {
        Label content = label("content");

        assertThat(children(ScrollableLayout.create(content))).containsExactly(content);
    }

    @Test
    public void aWrappedLayoutVisitsWhatItWrapsNow() {
        Label first = label("first");
        Label second = label("second");

        WrappedLayout layout = WrappedLayout.create(first);
        assertThat(children(layout)).containsExactly(first);

        layout.set(second);
        assertThat(children(layout)).containsExactly(second);

        layout.set((Component) null);
        assertThat(children(layout)).isEmpty();
    }

    @Test
    public void aTwoComponentSplitVisitsItsFirstSideBeforeItsSecondWhateverOrderTheyWereSetIn() {
        Label first = label("first");
        Label second = label("second");

        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        layout.setSecondComponent(second);
        layout.setFirstComponent(first);

        assertThat(children(layout)).containsExactly(first, second);
    }

    @Test
    public void aThreeComponentSplitVisitsFirstCenterSecondAndSkipsAClearedPane() {
        Label first = label("first");
        Label center = label("center");
        Label second = label("second");

        ThreeComponentSplitLayout layout = ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        layout.setSecondComponent(second);
        layout.setCenterComponent(center);
        layout.setFirstComponent(first);

        assertThat(children(layout)).containsExactly(first, center, second);

        layout.setCenterComponent(null);
        assertThat(children(layout)).containsExactly(first, second);
    }

    @Test
    public void aTabbedLayoutVisitsTheContentOfEveryTabInTabOrder() {
        Label general = label("general");
        Label advanced = label("advanced");
        Label hidden = label("hidden");

        TabbedLayout layout = TabbedLayout.create();
        layout.addTab(LocalizeValue.of("General"), general);
        Tab advancedTab = layout.addTab(LocalizeValue.of("Advanced"), advanced);
        layout.addTab(LocalizeValue.of("Hidden"), hidden);

        assertThat(children(layout)).containsExactly(general, advanced, hidden);

        layout.removeTab(advancedTab);
        assertThat(children(layout)).containsExactly(general, hidden);
    }

    @Test
    public void aSwipeLayoutVisitsThePagesBuiltSoFarAndNeverBuildsOneToAnswer() {
        VerticalLayout first = VerticalLayout.create();
        VerticalLayout second = VerticalLayout.create();
        List<String> built = new ArrayList<>();

        SwipeLayout layout = SwipeLayout.create();
        layout.register("first", () -> {
            built.add("first");
            return first;
        });
        layout.register("second", () -> {
            built.add("second");
            return second;
        });

        assertThat(children(layout)).containsExactly(first);
        assertThat(built).containsExactly("first");

        layout.swipeLeftTo("second");
        assertThat(children(layout)).containsExactly(first, second);
    }

    @Test
    public void aLoadingLayoutVisitsItsInnerLayout() {
        Disposable parent = Disposable.newDisposable();
        try {
            VerticalLayout inner = VerticalLayout.create();

            LoadingLayout<VerticalLayout> layout = LoadingLayout.create(inner, parent);

            assertThat(children(layout)).containsExactly(inner);
        }
        finally {
            Disposer.dispose(parent);
        }
    }

    @Test
    public void aPlaceholderLayoutVisitsItsLayersInLayerOrder() {
        Label content = label("content");
        Label placeholder = label("placeholder");

        PlaceholderLayout layout = PlaceholderLayout.create();
        layout.add(content, PlaceholderLayout.Layer.CONTENT);
        layout.add(placeholder, PlaceholderLayout.Layer.PLACEHOLDER);

        assertThat(children(layout)).containsExactly(placeholder, content);
    }

    @Test
    public void disablingASplitRecursivelyReachesItsPanes() {
        Label first = label("first");
        Label second = label("second");

        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL)
            .withFirstComponent(first)
            .withSecondComponent(second);

        layout.setEnabledRecursive(false);

        assertThat(first.isEnabled()).isFalse();
        assertThat(second.isEnabled()).isFalse();
    }

    private static Label label(String text) {
        return Label.create(LocalizeValue.of(text));
    }

    private static List<Component> children(Layout<?> layout) {
        List<Component> children = new ArrayList<>();
        layout.forEachChild(children::add);
        return children;
    }
}
