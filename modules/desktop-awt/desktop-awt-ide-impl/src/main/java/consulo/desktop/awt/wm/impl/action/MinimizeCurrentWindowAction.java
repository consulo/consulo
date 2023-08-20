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
package consulo.desktop.awt.wm.impl.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.SystemInfo;
import consulo.desktop.awt.wm.impl.MacMainFrameDecorator;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * User: spLeaner
 */
@ActionImpl(id = "MinimizeCurrentWindow", parents = @ActionParentRef(value = @ActionRef(id = "WindowMenu"), anchor = ActionRefAnchor.FIRST))
public class MinimizeCurrentWindowAction extends AnAction implements DumbAware {

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final Component focusOwner = IdeFocusManager.getGlobalInstance().getFocusOwner();
    if (focusOwner != null) {
      final Window window = focusOwner instanceof JFrame ? (Window) focusOwner : SwingUtilities.getWindowAncestor(focusOwner);
      if (window instanceof JFrame && !(((JFrame)window).getState() == Frame.ICONIFIED)) {
        ((JFrame)window).setState(Frame.ICONIFIED);
      }
    }
  }

  @Override
  public void update(final AnActionEvent e) {
    final Presentation p = e.getPresentation();
    p.setVisible(SystemInfo.isMac);

    if (SystemInfo.isMac) {
      Project project = e.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        JFrame frame = (JFrame)TargetAWT.to(WindowManager.getInstance().getWindow(project));
        if (frame != null) {
          JRootPane pane = frame.getRootPane();
          p.setEnabled(pane != null && pane.getClientProperty(MacMainFrameDecorator.FULL_SCREEN) == null);
        }
      }
    }
    else {
      p.setEnabled(false);
    }
  }
}
