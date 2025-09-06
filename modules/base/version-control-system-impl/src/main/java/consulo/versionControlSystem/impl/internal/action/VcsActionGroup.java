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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author mike
 */
@ActionImpl(id = VcsActionGroup.ID)
public class VcsActionGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "VcsGroup";

    public VcsActionGroup() {
        super(ActionLocalize.groupVcsgroupText(), false);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        Project project = event.getData(Project.KEY);
        event.getPresentation().setEnabledAndVisible(project != null && project.isOpen());
    }
}
