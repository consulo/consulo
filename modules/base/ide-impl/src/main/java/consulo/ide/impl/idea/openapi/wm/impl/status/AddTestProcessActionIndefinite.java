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
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddTestProcessActionIndefinite extends AnAction implements DumbAware {
    public AddTestProcessActionIndefinite() {
        super("Add Test Process");
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(Project.KEY);

        new Task.Backgroundable(project, "Test", true) {
            @Override
            public void run(@Nonnull final ProgressIndicator indicator) {
                try {
                    Thread.currentThread().sleep(6000);

                    countTo(900, each -> {
                        indicator.setText("Found: " + each / 20 + 1);
                        if (each / 10.0 == Math.round(each / 10.0)) {
                            indicator.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
                        }
                        Thread.currentThread().sleep(10);
                        indicator.checkCanceled();
                        indicator.setText2("bla bla bla");
                    });
                    indicator.stop();
                }
                catch (Exception e1) {
                    indicator.stop();
                }
            }
        }.queue();
    }

    private void countTo(int top, AddTestProcessActionIndefinite.Count count) throws Exception {
        for (int i = 0; i < top; i++) {
            count.onCount(i);
        }
    }

    private static interface Count {
        void onCount(int each) throws InterruptedException;
    }
}
