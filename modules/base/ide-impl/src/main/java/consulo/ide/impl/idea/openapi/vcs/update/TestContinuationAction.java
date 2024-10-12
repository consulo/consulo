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
package consulo.ide.impl.idea.openapi.vcs.update;

import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.util.continuation.*;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 * @since 2011-04-07
 */
public class TestContinuationAction extends AnAction {
    public TestContinuationAction() {
        super("Test Continuation", "Test Continuation", PlatformIconGroup.nodesTag());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getDataContext().getData(Project.KEY);
        if (project == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            "Test Continuation",
            true,
            new PerformInBackgroundOption() {
                @Override
                public boolean shouldStartInBackground() {
                    return false;
                }

                @Override
                public void processSentToBackground() {
                }
            }
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                final Continuation continuation = Continuation.createForCurrentProgress(project, true, e.getPresentation().getText());
                final ReportTask finalBlock = new ReportTask("I'm finally block!");
                finalBlock.setHaveMagicCure(true);
                continuation.run(
                    new LongTaskDescriptor("First"),
                    new ReportTask("First complete"),
                    new TaskDescriptor("Adding task", Where.POOLED) {
                        @Override
                        public void run(final ContinuationContext context) {
                            addMore(context);
                            try {
                                Thread.sleep(10000);
                            }
                            catch (InterruptedException e1) {
                                //
                            }
                        }
                    },
                    new LongTaskDescriptor("Second"), new ReportTask("Second complete"),
                    new TaskDescriptor("Adding task 2", Where.POOLED) {
                        @Override
                        public void run(final ContinuationContext context) {
                            addMoreSurviving(context);
                            try {
                                Thread.sleep(10000);
                            }
                            catch (InterruptedException e1) {
                                //
                            }
                            throw new IllegalStateException();
                            /*context.suspend();
                            ApplicationManager.getApplication().invokeLater(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                      context.ping();
                                    }
                                },
                                ModalityState.NON_MODAL
                            );*/
                        }
                    },
                    new LongTaskDescriptor("Third"),
                    new ReportTask("Third complete"),
                    finalBlock);
            }

            @RequiredUIAccess
            @Override
            public void onCancel() {
                Messages.showInfoMessage("cancel!", myTitle.get());
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                Messages.showInfoMessage("success!", myTitle.get());
            }
        });
    }

    private void addMore(ContinuationContext context) {
        context.next(new LongTaskDescriptor("Inside killable"), new ReportTask("Inside killable complete"));
    }

    private void addMoreSurviving(ContinuationContext context) {
        final ContinuationFinalTasksInserter finalTasksInserter = new ContinuationFinalTasksInserter(context);
        finalTasksInserter.allNextAreFinal();
        context.next(new LongTaskDescriptor("Inside surviving"), new ReportTask("Inside surviving complete"));
        finalTasksInserter.removeFinalPropertyAdder();
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        final Project project = e.getDataContext().getData(Project.KEY);
        e.getPresentation().setEnabled(project != null);
    }

    private static class ReportTask extends TaskDescriptor {
        private ReportTask(String name) {
            super(name, Where.AWT);
        }

        @Override
        @RequiredUIAccess
        public void run(ContinuationContext context) {
            Messages.showInfoMessage(getName(), "Result");
        }
    }

    private static class LongTaskDescriptor extends TaskDescriptor {
        private LongTaskDescriptor(final String name) {
            super(name, Where.POOLED);
        }

        @Override
        public void run(ContinuationContext context) {
            final ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
            pi.setText(getName());
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                //
            }
            pi.setTextValue(LocalizeValue.empty());
        }
    }
}
