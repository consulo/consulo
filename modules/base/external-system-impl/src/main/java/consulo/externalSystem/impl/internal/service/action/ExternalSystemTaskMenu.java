// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalSystem.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.executor.Executor;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ActionImpl(
    id = "ExternalSystemView.TaskMenu",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE)
    }
)
public class ExternalSystemTaskMenu extends DefaultActionGroup implements DumbAware {
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Project project = e == null ? null : e.getData(Project.KEY);
        if (project == null) {
            return super.getChildren(e);
        }

        ActionManager actionManager = ActionManager.getInstance();
        List<AnAction> actions = new ArrayList<>();
        project.getApplication().getExtensionPoint(Executor.class).forEach(executor -> {
            if (executor.isApplicable(project)) {
                AnAction action = actionManager.getAction(executor.getContextActionId());
                if (action != null) {
                    actions.add(action);
                }
            }
        });
        Collections.addAll(actions, super.getChildren(e));
        return actions.toArray(AnAction.EMPTY_ARRAY);
    }
}
