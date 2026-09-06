// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.project.startup.StartupActivity;

import java.util.List;

final class InitialDumbTaskRequiredForSmartMode extends DumbModeTask {
    private static final Logger LOG = Logger.getInstance(InitialDumbTaskRequiredForSmartMode.class);

    private final Project myProject;

    InitialDumbTaskRequiredForSmartMode(Project project) {
        myProject = project;
    }

    @Override
    public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
        List<StartupActivity.RequiredForSmartMode> activities =
            myProject.getExtensionPoint(StartupActivity.RequiredForSmartMode.class).getExtensionList();
        for (StartupActivity.RequiredForSmartMode activity : activities) {
            ProgressManager.checkCanceled();

            LOG.assertTrue(
                indicator == ProgressIndicatorProvider.getGlobalProgressIndicator(),
                "There might be visual inconsistencies: " + "launched activities can only use thread's global indicator."
            );
            indicator.pushState();
            try {
                LOG.info("Running task required for smart mode: " + activity);
                activity.runActivity(myProject);
            }
            finally {
                indicator.popState();
                LOG.info("Finished task required for smart mode: " + activity);
            }
        }
    }
}
