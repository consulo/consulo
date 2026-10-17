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
package consulo.ide.impl.idea.application.options;

import consulo.annotation.component.ServiceImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.SearchableConfigurable;
import consulo.ide.impl.idea.application.options.codeStyle.OtherFileTypesCodeStyleConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.codeStyle.ui.internal.CodeStyleSettingsNavigator;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@Singleton
@ServiceImpl
public class CodeStyleSettingsNavigatorImpl implements CodeStyleSettingsNavigator {
    private final ShowSettingsUtil myShowSettingsUtil;

    @Inject
    public CodeStyleSettingsNavigatorImpl(ShowSettingsUtil showSettingsUtil) {
        myShowSettingsUtil = showSettingsUtil;
    }

    @RequiredUIAccess
    @Override
    public void showIndentSettings(Project project, LocalizeValue languageName) {
        CodeStyleSchemesConfigurable topConfigurable = new CodeStyleSchemesConfigurable(project);
        SearchableConfigurable found = topConfigurable.findSubConfigurable(languageName);
        if (found == null) {
            found = topConfigurable.findSubConfigurable(OtherFileTypesCodeStyleConfigurable.getDisplayNameText());
        }

        if (found instanceof CodeStyleSchemesConfigurable.CodeStyleConfigurableWrapper wrapper) {
            myShowSettingsUtil.editConfigurable(project, wrapper, () -> wrapper.selectTab(ApplicationLocalize.titleTabsAndIndents()));
        }
    }
}
