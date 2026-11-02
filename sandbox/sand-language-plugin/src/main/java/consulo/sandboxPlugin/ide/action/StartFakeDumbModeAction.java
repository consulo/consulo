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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.progress.ProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.DumbModeTask;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;

/**
 * Keeps the project in dumb mode for a while, to look at what the frame shows during indexing.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ActionImpl(id = "SandStartFakeDumbMode", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class StartFakeDumbModeAction extends DumbAwareAction {
    private static final int SECONDS = 25;

    public StartFakeDumbModeAction() {
        super(LocalizeValue.localizeTODO("Start Fake Dumb Mode"));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        DumbService.getInstance(project).queueTask(new DumbModeTask() {
            @Override
            public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
                indicator.setIndeterminate(false);
                int steps = SECONDS * 10;
                for (int step = 0; step < steps; step++) {
                    indicator.checkCanceled();
                    indicator.setFraction((double) step / steps);
                    indicator.setText(LocalizeValue.localizeTODO("Fake indexing"));
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });
    }
}
