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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.Length;
import consulo.ui.MessageBoxes;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TileLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.TimeoutUtil;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "layouts")
public class LayoutsUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Layouts");
    }

    @Override
    public boolean isClosable() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        TabbedLayout tabbedLayout = TabbedLayout.create();

        VerticalLayout fold = VerticalLayout.create();
        fold.add(Label.create("Some label"));
        fold.add(Button.create(
            LocalizeValue.localizeTODO("Some &Button"),
            e -> MessageBoxes.okError(LocalizeValue.of("Clicked!")).showAsync()
        ));

        FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Show Me"), fold);
        layout.addOpenedListener(it -> MessageBoxes.okInfo(LocalizeValue.of("State " + it.isOpened())).showAsync());

        tabbedLayout.addTab(LocalizeValue.localizeTODO("FoldoutLayout"), layout);

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(DockLayout.create().center(Button.create("Left")));
        splitLayout.setSecondComponent(DockLayout.create().center(Button.create("Second")));

        tabbedLayout.addTab(LocalizeValue.localizeTODO("SplitLayout"), splitLayout);

        SwipeLayout swipeLayout = SwipeLayout.create();

        swipeLayout.register("left", () -> swipeChildLayout(LocalizeValue.of("Right"), () -> swipeLayout.swipeRightTo("right")));
        swipeLayout.register("right", () -> swipeChildLayout(LocalizeValue.of("Left"), () -> swipeLayout.swipeLeftTo("left")));

        tabbedLayout.addTab(LocalizeValue.localizeTODO("SwipeLayout"), swipeLayout);

        VerticalLayout borderLayout = VerticalLayout.create();
        DockLayout dockLayout = DockLayout.create();
        Button centerBtn = Button.create(LocalizeValue.of("Center"));
        centerBtn.addClickListener(
            event -> dockLayout.center(HorizontalLayout.create().add(Label.create(LocalizeValue.of(LocalDateTime.now().toString()))))
        );

        borderLayout.add(centerBtn).add(dockLayout);

        tabbedLayout.addTab(LocalizeValue.localizeTODO("DockLayout"), borderLayout);
        tabbedLayout.addTab(LocalizeValue.localizeTODO("LoadingLayout"), loadingLayout(uiDisposable));
        tabbedLayout.addTab(LocalizeValue.localizeTODO("TileLayout"), tileLayout());
        tabbedLayout.addTab(LocalizeValue.localizeTODO("Card"), cards());

        return tabbedLayout;
    }

    @RequiredUIAccess
    private static Component tileLayout() {
        RadioGroup<String> group = RadioGroup.create();

        TileLayout pair = TileLayout.create(Length.ofFont(12));
        pair.add(tile(group, "Light", 2));
        pair.add(tile(group, "Dark", 3));
        group.setValue("Light");

        TileLayout tiles = TileLayout.create(Length.ofFont(10));
        List<Component> added = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            Component tile = tile(null, "Tile " + i, i % 4 + 1);
            added.add(tile);
            tiles.add(tile);
        }

        Button add = Button.create(LocalizeValue.of("Add Tile"), event -> {
            Component tile = tile(null, "Tile " + (added.size() + 1), added.size() % 4 + 1);
            added.add(tile);
            tiles.add(tile);
        });
        Button remove = Button.create(LocalizeValue.of("Remove Last Tile"), event -> {
            if (!added.isEmpty()) {
                tiles.remove(added.remove(added.size() - 1));
            }
        });

        DockLayout layout = DockLayout.create();
        layout.top(VerticalLayout.create().add(pair).add(HorizontalLayout.create().add(add).add(remove)));
        layout.center(ScrollableLayout.create(tiles));
        return layout;
    }

    @RequiredUIAccess
    private static Component cards() {
        RadioGroup<String> theme = RadioGroup.create();

        TileLayout themes = TileLayout.create(Length.ofFont(12));
        themes.add(choiceCard(theme, "Light", "Bright background", PlatformIconGroup.lafsLightpreview()));
        themes.add(choiceCard(theme, "Dark", "Easy on the eyes at night", PlatformIconGroup.lafsDarkpreview()));
        theme.setValue("Light");

        Card plain = Card.create();
        plain.setTitle(LocalizeValue.of("Default"));
        plain.setSubtitle(LocalizeValue.of("Title and subtitle"));

        Card outlined = Card.create();
        outlined.addStyle(CardStyle.OUTLINED);
        outlined.setTitle(LocalizeValue.of("Outlined"));
        outlined.add(Label.create(LocalizeValue.of("Content below the header")));

        Card elevated = Card.create();
        elevated.addStyle(CardStyle.ELEVATED);
        elevated.setTitle(LocalizeValue.of("Elevated"));
        elevated.addToFooter(Button.create(LocalizeValue.of("Footer Action")));

        Card horizontal = Card.create();
        horizontal.addStyle(CardStyle.HORIZONTAL);
        horizontal.setMedia(ImageBox.create(PlatformIconGroup.nodesPlugin()));
        horizontal.setTitle(LocalizeValue.of("Horizontal"));
        horizontal.setSubtitle(LocalizeValue.of("Media beside the text"));

        CheckBox checkBox = CheckBox.create(LocalizeValue.empty());
        Card checkable = Card.create();
        checkable.setHeaderPrefix(checkBox);
        checkable.setTitle(LocalizeValue.of("Checkable"));
        checkable.setSubtitle(LocalizeValue.of("Click anywhere on the card"));
        checkable.addClickListener(event -> checkBox.setValue(!checkBox.getValue()));

        TileLayout variants = TileLayout.create(Length.ofFont(12));
        variants.add(plain).add(outlined).add(elevated).add(horizontal).add(checkable);

        VerticalLayout layout = VerticalLayout.create(Space.LARGE);
        layout.add(themes);
        layout.add(variants);
        return ScrollableLayout.create(layout);
    }

    @RequiredUIAccess
    private static Card choiceCard(RadioGroup<String> group, String value, String description, Image preview) {
        Card card = Card.create();
        card.setHeaderPrefix(group.newButton(LocalizeValue.empty(), value));
        card.setTitle(LocalizeValue.of(value));
        card.setSubtitle(LocalizeValue.of(description));
        card.setMedia(ImageBox.create(preview));
        card.addClickListener(event -> group.setValue(value));
        return card;
    }

    @RequiredUIAccess
    private static Component tile(@Nullable RadioGroup<String> group, String title, int lines) {
        VerticalLayout tile = VerticalLayout.create(Space.SMALL);
        if (group != null) {
            tile.add(group.newButton(LocalizeValue.of(title), title));
        }
        else {
            tile.add(Label.create(LocalizeValue.of(title)));
        }
        for (int line = 1; line < lines; line++) {
            tile.add(Label.create(LocalizeValue.of("Line " + line)));
        }
        tile.borderBuilder().allSet().apply();
        tile.paddingBuilder().allSet(Space.LARGE).apply();
        return tile;
    }

    @RequiredUIAccess
    private static Layout swipeChildLayout(LocalizeValue text, @RequiredUIAccess Runnable runnable) {
        DockLayout dockLayout = DockLayout.create();

        dockLayout.center(HorizontalLayout.create().add(Button.create(text, e -> runnable.run())));

        return dockLayout;
    }

    @RequiredUIAccess
    private static Component loadingLayout(Disposable uiDisposable) {
        DockLayout layout = DockLayout.create();

        DockLayout innerLayout = DockLayout.create();

        LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(innerLayout, uiDisposable);

        Button start = Button.create(LocalizeValue.of("Start"), event -> loadingLayout.startLoading());

        Button stop = Button.create(
            LocalizeValue.of("Stop"),
            event -> loadingLayout.stopLoading(dockLayout -> {
                dockLayout.removeAll();

                dockLayout.center(Label.create(LocalizeValue.of(LocalDateTime.now().toString())));
            })
        );

        Button startPooled = Button.create(
            LocalizeValue.of("Start Pooled"),
            event -> loadingLayout.startLoading(
                () -> {
                    TimeoutUtil.sleep(10000);
                    return "Some Value after 10 seconds";
                },
                (dockLayout, someValue) -> dockLayout.center(Label.create(LocalizeValue.of(someValue)))
            )
        );

        layout.top(HorizontalLayout.create().add(start).add(stop).add(startPooled));
        layout.center(loadingLayout);
        return layout;
    }
}
