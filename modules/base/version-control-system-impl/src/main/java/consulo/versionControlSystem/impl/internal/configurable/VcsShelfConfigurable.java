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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * @author VISTALL
 * @since 2026-09-03
 */
public class VcsShelfConfigurable extends SimpleConfigurableByProperties implements SearchableConfigurable {
    private final Project myProject;

    public VcsShelfConfigurable(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);

        VerticalLayout root = VerticalLayout.create();

        CheckBox baseRevisionTextsBox =
            CheckBox.create(VcsLocalize.settingsStoreBaseRevisionTexts(), settings.INCLUDE_TEXT_INTO_SHELF);
        propertyBuilder.add(baseRevisionTextsBox, () -> settings.INCLUDE_TEXT_INTO_SHELF, value -> settings.INCLUDE_TEXT_INTO_SHELF = value);
        root.add(baseRevisionTextsBox);

        Label noteLabel = Label.create(
            VcsLocalize.settingsFileTextsBiggerThanNotStored(VcsConfiguration.ourMaximumFileForBaseRevisionSize / 1000)
        );
        noteLabel.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        noteLabel.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 20);
        root.add(noteLabel);

        return root;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return VcsLocalize.settingsShelfConfigurableName();
    }

    @Override
    public String getId() {
        return "Shelf.Project.Settings";
    }
}
