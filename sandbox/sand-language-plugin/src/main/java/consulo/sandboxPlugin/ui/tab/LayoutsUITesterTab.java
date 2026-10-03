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
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.TimeoutUtil;

import java.time.LocalDateTime;

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

        tabbedLayout.addTab("FoldoutLayout", layout);

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(DockLayout.create().center(Button.create("Left")));
        splitLayout.setSecondComponent(DockLayout.create().center(Button.create("Second")));

        tabbedLayout.addTab("SplitLayout", splitLayout);

        SwipeLayout swipeLayout = SwipeLayout.create();

        swipeLayout.register("left", () -> swipeChildLayout(LocalizeValue.of("Right"), () -> swipeLayout.swipeRightTo("right")));
        swipeLayout.register("right", () -> swipeChildLayout(LocalizeValue.of("Left"), () -> swipeLayout.swipeLeftTo("left")));

        tabbedLayout.addTab("SwipeLayout", swipeLayout);

        VerticalLayout borderLayout = VerticalLayout.create();
        DockLayout dockLayout = DockLayout.create();
        Button centerBtn = Button.create(LocalizeValue.of("Center"));
        centerBtn.addClickListener(
            event -> dockLayout.center(HorizontalLayout.create().add(Label.create(LocalizeValue.of(LocalDateTime.now().toString()))))
        );

        borderLayout.add(centerBtn).add(dockLayout);

        tabbedLayout.addTab("DockLayout", borderLayout);
        tabbedLayout.addTab("LoadingLayout", loadingLayout(uiDisposable));

        return tabbedLayout;
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
