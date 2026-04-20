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
package consulo.compiler.impl.internal;

import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.ExitStatus;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.AppIconScheme;
import consulo.ui.ex.awt.UIUtil;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Eugene Zhuravlev
 * @since 2003-01-22
 */
public class CompilerTask extends Task.Backgroundable {
    private static final String APP_ICON_ID = "compiler";

    private final boolean myWaitForPreviousSession;

    private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
    private Consumer<BuildProgress<BuildProgressDescriptor>> myCompileWork;
    private final boolean myCompilationStartedAutomatically;
    private final CompileCounters myCounters;
    private final UUID mySessionId;

    private final BuildViewServiceImpl myBuildViewService;
    private long myEndCompilationStamp;
    private ExitStatus myExitStatus;

    public CompilerTask(
        Project project,
        LocalizeValue contentName,
        boolean waitForPreviousSession,
        boolean compilationStartedAutomatically,
        CompileCounters counters
    ) {
        super(project, contentName);
        myWaitForPreviousSession = waitForPreviousSession;
        myCompilationStartedAutomatically = compilationStartedAutomatically;
        myCounters = counters;
        mySessionId = UUID.randomUUID();
        myBuildViewService = new BuildViewServiceImpl(project, mySessionId, contentName.get(), counters);
    }

    public ProgressIndicator getIndicator() {
        return myIndicator;
    }

    public void setEndCompilationStamp(ExitStatus exitStatus, long endCompilationStamp) {
        myExitStatus = exitStatus;
        myEndCompilationStamp = endCompilationStamp;
    }

    @Override
    public @Nullable NotificationInfo getNotificationInfo() {
        int errors = myCounters.get(CompilerMessageCategory.ERROR);
        int warnings = myCounters.get(CompilerMessageCategory.WARNING);

        return new NotificationInfo(
            errors > 0 ? "Compiler (errors)" : "Compiler (success)",
            LocalizeValue.localizeTODO("Compilation Finished"),
            LocalizeValue.localizeTODO(errors + " Errors, " + warnings + " Warnings"),
            true
        );
    }

    @Override
    public void run(ProgressIndicator indicator) {
        myIndicator = indicator;

        long startCompilationStamp = System.currentTimeMillis();

        indicator.setIndeterminate(false);

        myBuildViewService.onStart(mySessionId, startCompilationStamp, null, indicator);

        Semaphore semaphore = ((CompilerManagerImpl) CompilerManager.getInstance((Project) myProject)).getCompilationSemaphore();
        boolean acquired = false;
        try {
            try {
                while (!acquired) {
                    acquired = semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
                    if (!acquired && !myWaitForPreviousSession) {
                        return;
                    }
                    if (indicator.isCanceled()) {
                        // give up obtaining the semaphore,
                        // let compile work begin in order to stop gracefuly on cancel event
                        break;
                    }
                }
            }
            catch (InterruptedException ignored) {
            }

            if (!isHeadless()) {
                addIndicatorDelegate();
            }
            myCompileWork.accept(myBuildViewService.getBuildProgress());
        }
        catch (ProcessCanceledException ignored) {
        }
        finally {
            try {
                indicator.stop();

                myBuildViewService.onEnd(mySessionId, myExitStatus, myEndCompilationStamp);
            }
            finally {
                if (acquired) {
                    semaphore.release();
                }
            }
        }
    }

    private void addIndicatorDelegate() {
        ProgressIndicator indicator = myIndicator;
        if (!(indicator instanceof ProgressIndicatorEx progressIndicatorEx)) {
            return;
        }
        progressIndicatorEx.addStateDelegate(new AbstractProgressIndicatorExBase() {
            @Override
            public void cancel() {
                super.cancel();
                stopAppIconProgress();
            }

            @Override
            public void stop() {
                super.stop();
                stopAppIconProgress();
            }

            private void stopAppIconProgress() {
                UIUtil.invokeLaterIfNeeded(() -> {
                    AppIcon appIcon = AppIcon.getInstance();
                    if (appIcon.hideProgress(myProject, APP_ICON_ID)) {
                        int errors = myCounters.get(CompilerMessageCategory.ERROR);

                        if (errors > 0) {
                            appIcon.setErrorBadge(myProject, String.valueOf(errors));
                            appIcon.requestAttention(myProject, true);
                        }
                        else if (!myCompilationStartedAutomatically) {
                            appIcon.setOkBadge(myProject, true);
                            appIcon.requestAttention(myProject, false);
                        }
                    }
                });
            }

            @Override
            public void setFraction(double fraction) {
                super.setFraction(fraction);
                UIUtil.invokeLaterIfNeeded(
                    () -> AppIcon.getInstance().setProgress(myProject, APP_ICON_ID, AppIconScheme.Progress.BUILD, fraction, true)
                );
            }
        });
    }

    public void cancel() {
        if (!myIndicator.isCanceled()) {
            myIndicator.cancel();
        }
    }

    public void addMessage(CompilerMessage message) {
        myBuildViewService.addMessage(mySessionId, message);
    }

    public void start(Consumer<BuildProgress<BuildProgressDescriptor>> compileWork) {
        myCompileWork = compileWork;

        myBuildViewService.openBuildToolWindow();
        
        queue();
    }
}