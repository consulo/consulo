// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.Application;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.TaskInfo;
import consulo.application.util.registry.Registry;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.AppIconScheme;
import org.jspecify.annotations.Nullable;

final class DumbServiceAppIconProgress extends ProgressIndicatorBase {
    private final Project myProject;
    private double myLastFraction;

    DumbServiceAppIconProgress(Project project) {
        myProject = project;
    }

    static void registerForProgress(Project project, ProgressIndicatorEx indicator) {
        if (!Application.get().isHeadlessEnvironment()) {
            indicator.addStateDelegate(new DumbServiceAppIconProgress(project));
        }
    }

    @Override
    public void setFraction(double fraction) {
        if (fraction - myLastFraction < 0.01d) {
            return;
        }
        myLastFraction = fraction;
        UIAccess uiAccess = uiAccess();
        if (uiAccess == null) {
            return;
        }
        uiAccess.giveIfNeed(
            () -> AppIcon.getInstance().setProgress(myProject, "indexUpdate", AppIconScheme.Progress.INDEXING, fraction, true)
        );
    }

    @Override
    public void finish(TaskInfo task) {
        UIAccess uiAccess = uiAccess();
        if (myLastFraction != 0 && uiAccess != null) { // we should call setProgress at least once before
            uiAccess.giveIfNeed(() -> {
                AppIcon appIcon = AppIcon.getInstance();
                if (appIcon.hideProgress(myProject, "indexUpdate")) {
                    if (Registry.is("ide.appIcon.requestAttention.after.indexing", false)) {
                        appIcon.requestAttention(myProject, false);
                    }
                    appIcon.setOkBadge(myProject, true);
                }
            });
        }
    }

    /**
     * The project's UI access, or {@code null} once the project is closing: an indexing task may finish after its
     * project's frame is gone, and the icon of a closed project has nothing to show.
     */
    private @Nullable UIAccess uiAccess() {
        if (myProject.isDisposed()) {
            return null;
        }
        UIAccess uiAccess = myProject.getUIAccess();
        return uiAccess.isValid() ? uiAccess : null;
    }
}
