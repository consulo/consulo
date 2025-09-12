/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RevealFileAction extends DumbAwareAction {
    public RevealFileAction() {
        getTemplatePresentation().setTextValue(getActionName(null));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        VirtualFile file = ShowFilePathAction.findLocalFile(e.getData(VirtualFile.KEY));
        Presentation presentation = e.getPresentation();
        presentation.setTextValue(getActionName(e.getPlace()));
        presentation.setEnabled(file != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile file = ShowFilePathAction.findLocalFile(e.getRequiredData(VirtualFile.KEY));
        if (file == null) {
            return;
        }

        Platform platform = Platform.current();

        if (file.isDirectory()) {
            platform.openFileInFileManager(file.toNioPath(), UIAccess.current());
        } else {
            platform.openDirectoryInFileManager(file.toNioPath(), UIAccess.current());
        }
    }

    @Nonnull
    public static LocalizeValue getActionName() {
        return getActionName(null);
    }

    @Nonnull
    public static LocalizeValue getActionName(@Nullable String place) {
        String fileManagerName = Platform.current().fileManagerName();
        if (ActionPlaces.EDITOR_TAB_POPUP.equals(place)
            || ActionPlaces.EDITOR_POPUP.equals(place)
            || ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) {
            return LocalizeValue.ofNullable(fileManagerName);
        }
        return Platform.current().os().isMac()
            ? ActionLocalize.actionRevealinNameMac()
            : ActionLocalize.actionRevealinNameOther(fileManagerName);
    }
}
