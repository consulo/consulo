/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.ui;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.function.BooleanConsumer;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import jakarta.annotation.Nonnull;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 29/11/2025
 */
public class CheckBoxRefreshableOnComponent implements RefreshableOnComponent {
    private final CheckBox myCheckBox;
    @Nonnull
    private final BooleanSupplier myValueGetter;
    @Nonnull
    private final BooleanConsumer myValueSetter;

    @RequiredUIAccess
    public CheckBoxRefreshableOnComponent(@Nonnull LocalizeValue checkBoxText,
                                          @Nonnull BooleanSupplier valueGetter,
                                          @Nonnull BooleanConsumer valueSetter) {
        myValueSetter = valueSetter;
        myValueGetter = valueGetter;
        
        myCheckBox = CheckBox.create(checkBoxText);
    }

    @RequiredUIAccess
    public CheckBoxRefreshableOnComponent(@Nonnull LocalizeValue checkBoxText,
                                          @Nonnull Project project,
                                          @Nonnull LocalizeValue dumbToolTipText,
                                          @Nonnull BooleanSupplier valueGetter,
                                          @Nonnull BooleanConsumer valueSetter) {
        myValueSetter = valueSetter;
        myValueGetter = valueGetter;

        myCheckBox = CheckBox.create(checkBoxText);
        CheckinHandlerUtil.disableWhenDumb(project, myCheckBox, dumbToolTipText);
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        return DockLayout.create().left(myCheckBox);
    }

    @Override
    public void refresh() {

    }

    @Override
    public void saveState() {
        myValueSetter.accept(myCheckBox.getValue());
    }

    @Override
    @RequiredUIAccess
    public void restoreState() {
        myCheckBox.setValue(myValueGetter.getAsBoolean());
    }
}
