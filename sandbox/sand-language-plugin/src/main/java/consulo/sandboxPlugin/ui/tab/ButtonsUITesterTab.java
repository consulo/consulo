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
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@ExtensionImpl(id = "buttons", order = "before grid")
public class ButtonsUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Buttons");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        Label result = Label.create(LocalizeValue.of("Clicked: -"));

        Button plain = Button.create(LocalizeValue.of("Plain"), e -> result.setText(LocalizeValue.of("Clicked: Plain")));

        Button primary = Button.create(LocalizeValue.of("Primary"), e -> result.setText(LocalizeValue.of("Clicked: Primary")));
        primary.addStyle(ButtonStyle.PRIMARY);

        OptionButton noOptions = OptionButton.create(LocalizeValue.of("No options"),
            e -> result.setText(LocalizeValue.of("Clicked: No options")));

        layout.add(HorizontalLayout.create().add(plain).add(primary).add(noOptions));

        OptionButton commit = OptionButton.create(LocalizeValue.of("Commit"), e -> result.setText(LocalizeValue.of("Clicked: Commit")));
        commit.setOptions(options(result));

        OptionButton primaryCommit = OptionButton.create(LocalizeValue.of("Commit"),
            e -> result.setText(LocalizeValue.of("Clicked: Commit (primary)")));
        primaryCommit.addStyle(ButtonStyle.PRIMARY);
        primaryCommit.setOptions(options(result));

        OptionButton disabled = OptionButton.create(LocalizeValue.of("Disabled"));
        disabled.setOptions(options(result));
        disabled.setEnabled(false);

        layout.add(HorizontalLayout.create().add(commit).add(primaryCommit).add(disabled));

        layout.add(result);
        return layout;
    }

    private static List<OptionButtonItem> options(Label result) {
        return List.of(
            OptionButtonItem.of(LocalizeValue.of("Commit and Push..."), e -> result.setText(LocalizeValue.of("Clicked: Commit and Push"))),
            new OptionButtonItem(LocalizeValue.of("Create Patch..."), PlatformIconGroup.actionsDiff(), true,
                e -> result.setText(LocalizeValue.of("Clicked: Create Patch"))),
            new OptionButtonItem(LocalizeValue.of("Shelve"), null, false, e -> result.setText(LocalizeValue.of("Clicked: Shelve")))
        );
    }
}
