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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.internal.ProjectManagerEx;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

public class TemplateProjectStructureAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project defaultProject = ProjectManagerEx.getInstanceEx().getDefaultProject();
        ShowSettingsUtil.getInstance().showProjectStructureDialog(defaultProject);
    }
}