// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.actions;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

public class ShowSearchHistoryAction extends AnAction {
  public ShowSearchHistoryAction() {
    super(
      LocalizeValue.localizeTODO("Search History"),
      LocalizeValue.localizeTODO("Show search history popup"),
      PlatformIconGroup.actionsSearchwithhistory()
    );
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    //do nothing, it's just shortcut-holding action
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }
}
