/*
 * Copyright 2013-2021 consulo.io
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
package consulo.fileEditor.impl.internal;

import consulo.fileEditor.internal.VetoActionCloseHandler;
import consulo.application.ui.UISettings;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.virtualFileSystem.VirtualFile;


public class CloseTab extends LegacyDumbAwareAction {
    private final Project myProject;
    private final VirtualFile myFile;
    private final FileEditorWindow myEditorWindow;

    public CloseTab(Project project, VirtualFile file, FileEditorWindow editorWindow) {
        myProject = project;
        myFile = file;
        myEditorWindow = editorWindow;

        setShortcutSet(() -> KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_CLOSE).getShortcuts());
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
        boolean pinned = isPinned();

        Presentation presentation = e.getPresentation();
        if (!VetoActionCloseHandler.isClosable(myEditorWindow, myFile, myProject)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setIcon(pinned ? PlatformIconGroup.actionsPintab() : PlatformIconGroup.actionsClose());
        presentation.setVisible(UISettings.getInstance().getShowCloseButton() || pinned);

        if (pinned) {
            presentation.setText(IdeLocalize.actionUnpinTab());
            presentation.setDescription(IdeLocalize.actionUnpinTab());
        }
        else {
            LocalizeValue actionText = LocalizeValue.localizeTODO("Close. Alt-click to close others.");

            presentation.setText(actionText);
            presentation.setDescription(actionText);
        }
    }

    private boolean isPinned() {
        return myEditorWindow.isFilePinned(myFile);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!VetoActionCloseHandler.isClosable(myEditorWindow, myFile, myProject)) {
            return;
        }

        if (isPinned() && ActionPlaces.EDITOR_TAB.equals(e.getPlace())) {
            myEditorWindow.setFilePinned(myFile, false);
            return;
        }

        FileEditorManagerEx mgr = FileEditorManagerEx.getInstanceEx(myProject);
        FileEditorWindow window;
        VirtualFile file = myFile;
        if (ActionPlaces.EDITOR_TAB.equals(e.getPlace())) {
            window = myEditorWindow;
        }
        else {
            window = mgr.getCurrentWindow();
        }

        if (window != null) {
            if (e.getInputDetails() instanceof ModifiedInputDetails details && details.withAlt()) {
                window.closeAllExcept(file);
            }
            else {
                if (window.findFileComposite(file) != null) {
                    mgr.closeFile(file, window);
                }
            }
        }
    }
}
