/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class MarkObjectAction extends XDebuggerActionBase {
    private final XMarkObjectActionHandler myHandler = new XMarkObjectActionHandler();

    public MarkObjectAction() {
        super();
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        boolean enabled = false;
        Presentation presentation = event.getPresentation();
        boolean hidden = true;
        if (project != null) {
            hidden = myHandler.isHidden(project, event);
            if (myHandler.isEnabled(project, event)) {
                enabled = true;
                presentation.setTextValue(
                    myHandler.isMarked(project, event)
                        ? ActionLocalize.actionDebuggerMarkobjectUnmarkText()
                        : ActionLocalize.actionDebuggerMarkobjectText()
                );
            }
        }
        presentation.setVisible(!hidden && (!ActionPlaces.isPopupPlace(event.getPlace()) || enabled));
        presentation.setEnabled(enabled);
    }

    @Nonnull
    @Override
    protected DebuggerActionHandler getHandler() {
        return myHandler;
    }
}
