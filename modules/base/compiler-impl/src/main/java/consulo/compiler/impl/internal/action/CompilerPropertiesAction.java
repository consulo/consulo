/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.compiler.impl.internal.action;

import consulo.compiler.impl.internal.CompilerConfigurable;
import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author Eugene Zhuravlev
 * @since 2012-09-12
 */
public class CompilerPropertiesAction extends AnAction {
    public CompilerPropertiesAction() {
        super(CompilerLocalize.actionCompilerPropertiesText(), LocalizeValue.empty(), PlatformIconGroup.generalSettings());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            ProjectSettingsService.getInstance(project).showAndSelect(CompilerConfigurable.class);
        }
    }
}
