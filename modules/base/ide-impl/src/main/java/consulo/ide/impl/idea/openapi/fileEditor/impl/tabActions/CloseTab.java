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
package consulo.ide.impl.idea.openapi.fileEditor.impl.tabActions;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.openapi.ui.ShadowAction;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.lang.BitUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.InputEvent;

public class CloseTab extends AnAction implements DumbAware {
    private final Project myProject;
    private final VirtualFile myFile;
    private final FileEditorWindow myEditorWindow;

    public CloseTab(JComponent c, Project project, VirtualFile file, FileEditorWindow editorWindow) {
        myProject = project;
        myFile = file;
        myEditorWindow = editorWindow;
        new ShadowAction(this, ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE), c);
    }

    public CloseTab(Component c, Project project, VirtualFile file, FileEditorWindow editorWindow) {
        myProject = project;
        myFile = file;
        myEditorWindow = editorWindow;
        // TODO [VISTALL] unsupported new ShadowAction(this, ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE), c);
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
        boolean pinned = isPinned();

        Presentation presentation = e.getPresentation();
        presentation.setIcon(pinned ? PlatformIconGroup.actionsPintab() : PlatformIconGroup.actionsClose());
        presentation.setVisible(UISettings.getInstance().getShowCloseButton() || pinned);

        if (pinned) {
            presentation.setTextValue(IdeLocalize.actionUnpinTab());
            presentation.setDescriptionValue(IdeLocalize.actionUnpinTab());
        }
        else {
            LocalizeValue actionText = LocalizeValue.localizeTODO("Close. Alt-click to close others.");

            presentation.setTextValue(actionText);
            presentation.setDescriptionValue(actionText);
        }
    }

    private boolean isPinned() {
        return myEditorWindow.isFilePinned(myFile);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
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
            if (BitUtil.isSet(e.getModifiers(), InputEvent.ALT_DOWN_MASK)) {
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
