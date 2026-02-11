/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.IndentOptionsEditor;
import jakarta.annotation.Nonnull;

import javax.swing.*;

class IndentOptionsEditorWithSmartTabs extends IndentOptionsEditor {
    private JCheckBox myCbSmartTabs;

    @Override
    protected void addTabOptions() {
        super.addTabOptions();
        myCbSmartTabs = new JCheckBox(CodeStyleLocalize.checkboxIndentSmartTabs().get());
        add(myCbSmartTabs, true);
    }

    @Override
    public void reset(@Nonnull CodeStyleSettings settings, @Nonnull CommonCodeStyleSettings.IndentOptions options) {
        super.reset(settings, options);
        myCbSmartTabs.setSelected(options.SMART_TABS);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions options) {
        return super.isModified(settings, options) || isFieldModified(myCbSmartTabs, options.SMART_TABS);
    }

    @Override
    public void apply(CodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions options) {
        super.apply(settings, options);
        options.SMART_TABS = myCbSmartTabs.isSelected();
    }
}
