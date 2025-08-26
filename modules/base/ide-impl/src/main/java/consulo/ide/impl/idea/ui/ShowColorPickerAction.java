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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ActionImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColorChooser;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "ShowColorPicker")
public class ShowColorPickerAction extends AnAction {
    public ShowColorPickerAction() {
        super(ActionLocalize.actionShowcolorpickerText(), ActionLocalize.actionShowcolorpickerText(), PlatformIconGroup.idePipette());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        JComponent root = rootComponent(e.getData(Project.KEY));
        if (root != null) {
            ColorChooser.chooseColor(root, IdeLocalize.dialogTitleColorPicker().get(), null, true, true, color -> {
            });
        }
    }

    private static JComponent rootComponent(Project project) {
        if (project != null) {
            IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
            if (frame != null) {
                return frame.getComponent();
            }
        }

        JFrame frame = (JFrame) TargetAWT.to(WindowManager.getInstance().findVisibleWindow());
        return frame != null ? frame.getRootPane() : null;
    }
}
