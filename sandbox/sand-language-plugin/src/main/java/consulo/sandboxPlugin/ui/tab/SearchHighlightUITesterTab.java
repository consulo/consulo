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
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.HasSearchHighlight;
import consulo.ui.HtmlLabel;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Text components marking the runs a search query matches, found by walking the layouts the way settings search does,
 * plus the captions of the tabs and sections read off the layouts.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtensionImpl(id = "searchHighlight", order = "after delayedAction")
public class SearchHighlightUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Search highlight");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout options = VerticalLayout.create();
        options.add(Label.create(LocalizeValue.localizeTODO("Tab size: <four> spaces")));
        options.add(HtmlLabel.create(LocalizeValue.localizeTODO("<b>Use tab</b> character &amp; <i>smart</i> tabs")));
        options.add(CheckBox.create(LocalizeValue.localizeTODO("Use &tab character")));
        options.add(RadioButton.create(LocalizeValue.localizeTODO("Smart tabs"), true));

        Button resetButton = Button.create(LocalizeValue.localizeTODO("Reset tab size"));
        resetButton.setIcon(PlatformIconGroup.actionsRefresh());
        options.add(HorizontalLayout.create()
            .add(resetButton)
            .add(Button.create(LocalizeValue.localizeTODO("&Size"))));

        options.add(Hyperlink.create(LocalizeValue.localizeTODO("Configure tab size")));
        options.add(Hyperlink.create(LocalizeValue.localizeTODO("Edit <defaults> presets")));

        Label widthLabel = Label.create(LocalizeValue.localizeTODO("Tab &width:"));
        TextBox widthBox = TextBox.create("4");
        widthLabel.setTarget(widthBox);
        options.add(HorizontalLayout.create().add(widthLabel).add(widthBox));

        LabeledLayout section = LabeledLayout.create(
            LocalizeValue.localizeTODO("Tabs and Indents"),
            CheckBox.create(LocalizeValue.localizeTODO("Keep indents on empty lines"))
        );
        options.add(section);

        FoldoutLayout foldout = FoldoutLayout.create(
            LocalizeValue.localizeTODO("Advanced tab options"),
            Label.create(LocalizeValue.localizeTODO("Continuation indent size"))
        );
        options.add(foldout);

        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(LocalizeValue.localizeTODO("General"), Label.create(LocalizeValue.localizeTODO("Visible tab content")));
        tabs.addTab(CommonLocalize.titleSettings(), Label.create(LocalizeValue.localizeTODO("Hidden tab: size of tabs")));
        options.add(tabs);

        Label captions = Label.create(describeCaptions(tabs, section, foldout));

        TextBox query = TextBox.create("tab size");
        query.addValueListener(event -> highlight(options, event.getValue()));

        VerticalLayout root = VerticalLayout.create();
        root.add(HorizontalLayout.create().add(Label.create(LocalizeValue.localizeTODO("Query:"))).add(query));
        root.add(options);
        root.add(captions);

        highlight(options, query.getValue());
        return root;
    }

    private static LocalizeValue describeCaptions(TabbedLayout tabs, LabeledLayout section, FoldoutLayout foldout) {
        List<String> titles = new ArrayList<>();
        tabs.forEachTab((tab, content) -> titles.add(describe(tab.getTitle())));

        return LocalizeValue.of(
            "Tabs: " + String.join(", ", titles)
                + " | Section: " + describe(section.getLabel())
                + " | Foldout: " + describe(foldout.getTitle())
        );
    }

    private static String describe(LocalizeValue value) {
        Optional<LocalizeKey> key = value.getKey();
        if (key.isEmpty()) {
            return value.get();
        }

        LocalizeManager manager = LocalizeManager.get();
        String english = manager.getUnformattedText(key.get(), manager.getDefaultLocale());
        return value.get() + " (" + manager.getDefaultLocale().toLanguageTag() + ": " + english + ")";
    }

    @RequiredUIAccess
    private static void highlight(Component component, @Nullable String query) {
        if (component instanceof HasSearchHighlight highlight) {
            highlight.setSearchHighlight(query);
        }

        if (component instanceof Layout<?> layout) {
            layout.forEachChild(child -> highlight(child, query));
        }
    }
}
