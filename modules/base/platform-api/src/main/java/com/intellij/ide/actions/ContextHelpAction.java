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
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.DumbAware;

import javax.annotation.Nullable;

public class ContextHelpAction extends AnAction implements DumbAware {
  private final String myHelpID;

  public ContextHelpAction() {
    this(null);
  }

  public ContextHelpAction(@Nullable String helpID) {
    myHelpID = helpID;
  }

  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final String helpId = getHelpId(dataContext);
    if (helpId != null) {
      HelpManager.getInstance().invokeHelp(helpId);
    }
  }

  @Nullable
  protected String getHelpId(DataContext dataContext) {
    return myHelpID != null ? myHelpID : dataContext.getData(PlatformDataKeys.HELP_ID);
  }

  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();

    if (ActionPlaces.MAIN_MENU.equals(event.getPlace())) {
      DataContext dataContext = event.getDataContext();
      presentation.setEnabled(getHelpId(dataContext) != null);
    }
    else {
      presentation.setIcon(AllIcons.Actions.Help);
      presentation.setText(CommonBundle.getHelpButtonText());
    }
  }
}
