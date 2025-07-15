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

import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColorChooser;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class ShowColorPickerAction extends AnAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    JComponent root = rootComponent(e.getData(Project.KEY));
    if (root != null) {
      ColorChooser.chooseColor(root, "Color Picker", null, true, true, color -> {
      });
    }
  }

  private static JComponent rootComponent(Project project) {
    if (project != null) {
      IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
      if (frame != null) return frame.getComponent();
    }

    JFrame frame = WindowManager.getInstance().findVisibleFrame();
    return frame != null ? frame.getRootPane() : null;
  }
}
