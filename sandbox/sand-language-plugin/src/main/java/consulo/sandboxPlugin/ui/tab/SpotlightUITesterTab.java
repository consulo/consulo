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
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Spotlight;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtensionImpl(id = "spotlight", order = "after delayedAction")
public class SpotlightUITesterTab implements UITesterTab {
    private static final String INITIAL_QUERY = "show";

    private static final List<String> INSPECTIONS = List.of(
        "Unused import",
        "Unused declaration",
        "Redundant cast",
        "Show warning for empty catch blocks",
        "Constant conditions",
        "Line is longer than allowed",
        "Missing override annotation",
        "Field can be final",
        "Show hints for implicit types",
        "Duplicated code fragment",
        "Infinite recursion",
        "Unnecessary semicolon",
        "Pointless boolean expression",
        "Show problems in the scrollbar",
        "Spelling in comments",
        "Unused parameter",
        "Raw use of parameterized class",
        "Statement with empty body",
        "Show usages of deprecated API",
        "Method can be static",
        "Magic number",
        "Nested assignment",
        "Show inlay hints for parameters",
        "Unchecked warning",
        "Suspicious collection call"
    );

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Spotlight");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Map<Component, String> texts = new LinkedHashMap<>();

        VerticalLayout appearance = VerticalLayout.create();
        appearance.add(checkBox(texts, "Show line numbers", true));
        appearance.add(checkBox(texts, "Show method separators", false));
        appearance.add(checkBox(texts, "Show whitespaces", false));
        appearance.add(checkBox(texts, "Use soft wraps", false));
        appearance.add(labeledBox(texts, "Tab size:", 4));
        appearance.add(labeledBox(texts, "Right margin (columns):", 120));

        VerticalLayout tabs = VerticalLayout.create();
        tabs.add(checkBox(texts, "Show tabs in one row", true));
        tabs.add(checkBox(texts, "Mark modified tabs with asterisk", false));
        tabs.add(label(texts, "Tab placement: top"));

        VerticalLayout form = VerticalLayout.create();
        form.add(LabeledLayout.create(LocalizeValue.localizeTODO("Appearance"), appearance));
        form.add(LabeledLayout.create(LocalizeValue.localizeTODO("Editor Tabs"), tabs));

        VerticalLayout inspections = VerticalLayout.create();
        for (String inspection : INSPECTIONS) {
            inspections.add(checkBox(texts, inspection, false));
        }

        DockLayout inspectionsPanel = DockLayout.create();
        inspectionsPanel.top(label(texts, "Inspections (scroll to see every hit follow):"));
        inspectionsPanel.center(ScrollableLayout.create(inspections));

        DockLayout content = DockLayout.create();
        content.top(form);
        content.center(inspectionsPanel);

        Spotlight spotlight = Spotlight.over(content);
        Disposer.register(uiDisposable, spotlight);

        Label hits = Label.create();

        TextBox search = TextBox.create(INITIAL_QUERY).withPlaceholder(LocalizeValue.localizeTODO("Search options"));
        search.addValueListener(event -> highlight(spotlight, texts, hits, event.getValue()));

        highlight(spotlight, texts, hits, INITIAL_QUERY);

        HorizontalLayout searchRow = HorizontalLayout.create();
        searchRow.add(Label.create(LocalizeValue.localizeTODO("Search:")));
        searchRow.add(search);
        searchRow.add(hits);

        DockLayout root = DockLayout.create();
        root.top(searchRow);
        root.center(content);
        return root;
    }

    @RequiredUIAccess
    private static void highlight(Spotlight spotlight, Map<Component, String> texts, Label hits, @Nullable String value) {
        String query = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            spotlight.clear();
            hits.setText(LocalizeValue.empty());
            return;
        }

        List<Component> targets = new ArrayList<>();
        for (Map.Entry<Component, String> entry : texts.entrySet()) {
            if (entry.getValue().toLowerCase(Locale.ROOT).contains(query)) {
                targets.add(entry.getKey());
            }
        }

        spotlight.setTargets(targets);
        hits.setText(LocalizeValue.localizeTODO(targets.size() + " hit(s)"));
    }

    private static CheckBox checkBox(Map<Component, String> texts, String text, boolean selected) {
        CheckBox checkBox = CheckBox.create(LocalizeValue.of(text), selected);
        texts.put(checkBox, text);
        return checkBox;
    }

    private static Label label(Map<Component, String> texts, String text) {
        Label label = Label.create(LocalizeValue.of(text));
        texts.put(label, text);
        return label;
    }

    private static Component labeledBox(Map<Component, String> texts, String text, int value) {
        HorizontalLayout row = HorizontalLayout.create();
        row.add(label(texts, text));
        row.add(IntBox.create(value));
        return row;
    }
}
