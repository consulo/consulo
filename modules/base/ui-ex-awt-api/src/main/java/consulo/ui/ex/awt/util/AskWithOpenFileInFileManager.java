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
package consulo.ui.ex.awt.util;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author VISTALL
 * @since 2025-09-05
 */
public class AskWithOpenFileInFileManager {
    @RequiredUIAccess
    public static Boolean showDialog(Project project, LocalizeValue message, LocalizeValue title, File file) {
        Boolean[] ref = new Boolean[1];
        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return true;
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                if (!value) {
                    ref[0] = exitCode == 0;
                }
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return true;
            }

            @Nonnull
            @Override
            public LocalizeValue getDoNotShowMessage() {
                return CommonLocalize.dialogOptionsDoNotAsk();
            }
        };
        showDialog(project, message, title, file, option);
        return ref[0];
    }

    @RequiredUIAccess
    public static void showDialog(Project project, LocalizeValue message, LocalizeValue title, File file, DialogWrapper.DoNotAskOption option) {
        if (Messages.showOkCancelDialog(
            project,
            message.get(),
            title.get(),
            Platform.current().fileManagerName(),
            CommonLocalize.actionClose().get(),
            UIUtil.getInformationIcon(),
            option
        ) == Messages.OK) {
            Platform.current().openFileInFileManager(file, UIAccess.current());
        }
    }

    @Nonnull
    public static LocalizeValue getActionName(@Nullable String place) {
        if (ActionPlaces.EDITOR_TAB_POPUP.equals(place)
            || ActionPlaces.EDITOR_POPUP.equals(place)
            || ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) {
            return LocalizeValue.ofNullable(Platform.current().fileManagerName());
        }
        return Platform.current().os().isMac()
            ? ActionLocalize.actionRevealinNameMac()
            : ActionLocalize.actionRevealinNameOther(Platform.current().fileManagerName());
    }
}
