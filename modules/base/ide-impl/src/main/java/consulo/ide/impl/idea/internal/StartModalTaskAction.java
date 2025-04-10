/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.internal;

import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-11-23
 */
public class StartModalTaskAction extends DumbAwareAction {
    public StartModalTaskAction() {
        super("Start Modal Task");
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Task.Modal.queue(project, LocalizeValue.of("Running Modal Task"), true, progressIndicator -> {
            progressIndicator.setTextValue(LocalizeValue.of("Text Value 1..."));
            progressIndicator.setText2Value(LocalizeValue.of("Text Value 2..."));
            
            while (true) {
                if (progressIndicator.isCanceled()) {
                    break;
                }

                TimeoutUtil.sleep(1000);
            }
        });
    }
}
