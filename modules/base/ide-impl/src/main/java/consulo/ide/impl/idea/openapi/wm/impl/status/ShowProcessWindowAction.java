/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.dumb.DumbAware;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

import jakarta.annotation.Nonnull;

public class ShowProcessWindowAction extends ToggleAction implements DumbAware {

  public ShowProcessWindowAction() {
    super(ActionsBundle.message("action.ShowProcessWindow.text"), ActionsBundle.message("action.ShowProcessWindow.description"), null);
  }

  @Override
  public boolean isSelected(final AnActionEvent e) {
    IdeFrame frame = IdeFrameUtil.findFocusedRootIdeFrame();
    StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
    return statusBar != null && statusBar.isProcessWindowOpen();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(IdeFrameUtil.findFocusedRootIdeFrame() != null);
  }

  @Override
  public void setSelected(AnActionEvent e, final boolean state) {
    IdeFrame frame = IdeFrameUtil.findFocusedRootIdeFrame();
    StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();

    if(statusBar != null) {
      statusBar.setProcessWindowOpen(state);
    }
  }
}
