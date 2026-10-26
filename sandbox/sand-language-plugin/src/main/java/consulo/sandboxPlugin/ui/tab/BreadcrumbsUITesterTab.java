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
import consulo.ui.Breadcrumbs;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-26
 */
@ExtensionImpl(id = "breadcrumbs", order = "after badge")
public class BreadcrumbsUITesterTab implements UITesterTab {
    private static final Path DEEP_PATH = Path.of("/run/media/user/repositories/consulo/modules/base/ui-api/src/main/java/consulo/ui");

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Breadcrumbs");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Label clicked = Label.create(LocalizeValue.localizeTODO("Click a crumb"));

        Breadcrumbs<String> simple = Breadcrumbs.create();
        simple.setItems(List.of("Settings", "Editor", "General", "Appearance"));
        simple.addItemClickListener(event -> clicked.setText(LocalizeValue.of("Clicked: " + event.getItem())));

        Breadcrumbs<Path> path = Breadcrumbs.create();
        path.setItems(segments(DEEP_PATH));
        path.setRenderer((presentation, item) -> {
            Path value = item.getValue();
            if (value == null) {
                return;
            }
            Path fileName = value.getFileName();
            presentation.withIcon(PlatformIconGroup.nodesFolder());
            presentation.append(fileName == null ? value.toString() : fileName.toString());
            presentation.withTooltip(LocalizeValue.of(value.toString()));
        });
        path.addItemClickListener(event -> {
            Path item = event.getItem();
            clicked.setText(LocalizeValue.of("Clicked: " + item));
            path.setItems(segments(item));
        });

        Label resetHint = Label.create(LocalizeValue.localizeTODO("Clicking a path crumb trims the trail to it. Drag the splitter to make the trail too narrow."));

        // the splitter lets the trail be squeezed until the middle crumbs collapse
        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(path);
        split.setSecondComponent(Label.create(LocalizeValue.localizeTODO("drag the splitter")));
        split.setProportion(40);

        VerticalLayout root = VerticalLayout.create(Space.LARGE);
        root.add(simple);
        root.add(resetHint);
        root.add(split);
        root.add(clicked);
        return root;
    }

    private static List<Path> segments(Path path) {
        List<Path> result = new ArrayList<>();
        for (Path current = path; current != null; current = current.getParent()) {
            result.add(0, current);
        }
        return result;
    }
}
