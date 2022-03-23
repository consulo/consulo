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

import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.ToolWindowLayout;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author Vladimir Kondratyev
 */
public class StoreDefaultLayoutAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e){
    Project project = e.getData(CommonDataKeys.PROJECT);
    if(project==null){
      return;
    }
    ToolWindowLayout layout = ToolWindowManagerEx.getInstanceEx(project).getLayout();
    WindowManagerEx.getInstanceEx().setLayout(layout);
  }

  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    presentation.setEnabled(event.getData(CommonDataKeys.PROJECT) != null);
  }
}
