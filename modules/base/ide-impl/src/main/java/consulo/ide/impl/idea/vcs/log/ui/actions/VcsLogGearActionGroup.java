/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ide.impl.wm.impl.ToolWindowContentUI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.versionControlSystem.log.VcsLogUi;
import jakarta.annotation.Nonnull;

import java.awt.*;

public class VcsLogGearActionGroup extends DumbAwareAction {
  @Nonnull
  private final String myActionGroup;

  public VcsLogGearActionGroup(@Nonnull String group) {
    myActionGroup = group;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DefaultActionGroup group = new DefaultActionGroup(ActionManager.getInstance().getAction(myActionGroup));

    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
      null,
      group,
      e.getDataContext(),
      JBPopupFactory.ActionSelectionAid.MNEMONICS,
      true,
      ToolWindowContentUI.POPUP_PLACE
    );
    Component component = e.getInputEvent().getComponent();
    if (component instanceof ActionButtonComponent) {
      popup.showUnderneathOf(component);
    }
    else {
      popup.showInCenterOf(component);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    VcsLogUi logUi = e.getData(VcsLogUi.KEY);
    e.getPresentation().setEnabledAndVisible(project != null && logUi != null);
  }
}
