/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.project.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.internal.ProjectOpenSetting;
import consulo.ui.Alert;
import consulo.ui.AlertValueRemember;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProjectNewWindowDoNotAskOption implements DialogWrapper.DoNotAskOption, AlertValueRemember<Integer> {
    public static final ProjectNewWindowDoNotAskOption INSTANCE = new ProjectNewWindowDoNotAskOption();

    @Override
    public boolean isToBeShown() {
        return true;
    }

    @Override
    public void setToBeShown(boolean value, int exitCode) {
        int confirmOpenNewProject = value || exitCode == 2 ? ProjectOpenSetting.OPEN_PROJECT_ASK : exitCode == 0 ? ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW : ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW;
        ProjectOpenSetting.getInstance().setConfirmOpenNewProject(confirmOpenNewProject);
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
        return false;
    }

    @Nonnull
    @Override
    public LocalizeValue getDoNotShowMessage() {
        return CommonLocalize.dialogOptionsDoNotAsk();
    }

    @Override
    public void setValue(@Nonnull Integer value) {
        if (value == Alert.CANCEL) {
            return;
        }

        ProjectOpenSetting.getInstance().setConfirmOpenNewProject(value);
    }

    @Nullable
    @Override
    public Integer getValue() {
        int confirmOpenNewProject = ProjectOpenSetting.getInstance().getConfirmOpenNewProject();
        if (confirmOpenNewProject == ProjectOpenSetting.OPEN_PROJECT_ASK) {
            return null;
        }
        return confirmOpenNewProject;
    }

    @Nonnull
    @Override
    public String getMessageBoxText() {
        return CommonLocalize.dialogOptionsDoNotAsk().get();
    }
}