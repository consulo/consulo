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

import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * @author VISTALL
 * @since 2026-09-03
 */
public class VcsCommitConfigurable extends SimpleConfigurableByProperties implements SearchableConfigurable {
    private final Project myProject;

    public VcsCommitConfigurable(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VcsConfiguration settings = VcsConfiguration.getInstance(myProject);

        VerticalLayout root = VerticalLayout.create();

        CheckBox clearInitialCommitMessageBox = CheckBox.create(VcsLocalize.settingsClearInitialCommitMessage());
        propertyBuilder.add(
            clearInitialCommitMessageBox,
            () -> settings.CLEAR_INITIAL_COMMIT_MESSAGE,
            value -> settings.CLEAR_INITIAL_COMMIT_MESSAGE = value
        );
        root.add(clearInitialCommitMessageBox);

        CheckBox useMarginBox =
            CheckBox.create(VcsLocalize.configurationCommitMessageMarginPrompt(), settings.USE_COMMIT_MESSAGE_MARGIN);
        propertyBuilder.add(useMarginBox, () -> settings.USE_COMMIT_MESSAGE_MARGIN, value -> settings.USE_COMMIT_MESSAGE_MARGIN = value);

        IntBox marginSizeBox = IntBox.create(settings.COMMIT_MESSAGE_MARGIN_SIZE).withRange(0, 10000);
        propertyBuilder.add(
            marginSizeBox,
            () -> settings.COMMIT_MESSAGE_MARGIN_SIZE,
            value -> settings.COMMIT_MESSAGE_MARGIN_SIZE = value
        );
        root.add(VcsSettingsRows.gated(useMarginBox, marginSizeBox));

        CheckBox wrapOnMarginBox = CheckBox.create(
            ApplicationLocalize.checkboxWrapTypingOnRightMargin(),
            settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN
        );
        propertyBuilder.add(
            wrapOnMarginBox,
            () -> settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN,
            value -> settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = value
        );
        wrapOnMarginBox.setEnabled(settings.USE_COMMIT_MESSAGE_MARGIN);
        useMarginBox.addValueListener(event -> wrapOnMarginBox.setEnabled(event.getValue()));
        root.add(wrapOnMarginBox);

        CheckBox checkSpellingBox =
            CheckBox.create(VcsLocalize.settingsCheckCommitMessageSpelling(), settings.CHECK_COMMIT_MESSAGE_SPELLING);
        propertyBuilder.add(
            checkSpellingBox,
            () -> settings.CHECK_COMMIT_MESSAGE_SPELLING,
            value -> settings.CHECK_COMMIT_MESSAGE_SPELLING = value
        );
        root.add(checkSpellingBox);

        CheckBox showUnversionedBox = CheckBox.create(
            VcsLocalize.settingsShowUnversionedFilesInCommitDialog(),
            settings.SHOW_UNVERSIONED_FILES_WHILE_COMMIT
        );
        propertyBuilder.add(
            showUnversionedBox,
            () -> settings.SHOW_UNVERSIONED_FILES_WHILE_COMMIT,
            value -> settings.SHOW_UNVERSIONED_FILES_WHILE_COMMIT = value
        );
        root.add(showUnversionedBox);

        return root;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return VcsLocalize.settingsCommitConfigurableName();
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.CommitDialog";
    }
}
