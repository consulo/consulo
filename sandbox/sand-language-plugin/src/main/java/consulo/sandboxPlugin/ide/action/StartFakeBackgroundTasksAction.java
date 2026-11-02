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
import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;

/**
 * Starts several background tasks of different lengths - some with a fraction, some without - to look at the progress
 * part of the status bar and the list of running tasks.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ActionImpl(id = "SandStartFakeBackgroundTasks", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class StartFakeBackgroundTasksAction extends DumbAwareAction {
    private static final int COUNT = 6;

    public StartFakeBackgroundTasksAction() {
        super(LocalizeValue.localizeTODO("Start Fake Background Tasks"));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        for (int i = 0; i < COUNT; i++) {
            int index = i + 1;
            boolean determinate = i % 2 == 0;
            int seconds = 20 + i * 8;
            new Task.Backgroundable(project, LocalizeValue.localizeTODO("Fake task #" + index), true) {
                @Override
                public void run(ProgressIndicator indicator) {
                    indicator.setIndeterminate(!determinate);
                    int steps = seconds * 10;
                    for (int step = 0; step < steps; step++) {
                        indicator.checkCanceled();
                        if (determinate) {
                            indicator.setFraction((double) step / steps);
                        }
                        indicator.setText(LocalizeValue.localizeTODO("Step " + step + " of " + steps));
                        indicator.setText2(LocalizeValue.localizeTODO("file_" + (step % 37) + ".sand"));
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException ex) {
                            return;
                        }
                    }
                }
            }.queue();
        }
    }
}
