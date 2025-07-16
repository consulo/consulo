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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "XDebugger.Inline")
public class UseInlineDebuggerAction extends ToggleAction implements DumbAware {
  public UseInlineDebuggerAction() {
    super(ActionLocalize.actionXdebuggerInlineText(), ActionLocalize.actionXdebuggerInlineDescription());
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().isShowValuesInline();
  }

  @Override
  @RequiredUIAccess
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().setShowValuesInline(state);
    XDebuggerUtil.getInstance().rebuildAllSessionsViews(e.getData(Project.KEY));
  }
}
