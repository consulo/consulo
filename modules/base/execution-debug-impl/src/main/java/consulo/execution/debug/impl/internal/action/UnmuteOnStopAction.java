/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import jakarta.annotation.Nonnull;

/**
 * @author egor
 */
@ActionImpl(id = "XDebugger.UnmuteOnStop")
public class UnmuteOnStopAction extends ToggleAction implements DumbAware {
    public UnmuteOnStopAction() {
        super(XDebuggerLocalize.actionUnmuteOnStopText(), XDebuggerLocalize.actionUnmuteOnStopDescription());
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isUnmuteOnStop();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().setUnmuteOnStop(state);
    }
}