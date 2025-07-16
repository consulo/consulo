/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class SortValuesToggleAction extends ToggleAction implements DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    XDebugSession session = e.getData(XDebugSession.DATA_KEY);
    e.getPresentation().setEnabledAndVisible(session != null && !session.getDebugProcess().isValuesCustomSorted());
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().isSortValues();
  }

  @Override
  @RequiredUIAccess
  public void setSelected(AnActionEvent e, boolean state) {
    XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings().setSortValues(state);
    XDebuggerUtil.getInstance().rebuildAllSessionsViews(e.getData(Project.KEY));
  }
}
