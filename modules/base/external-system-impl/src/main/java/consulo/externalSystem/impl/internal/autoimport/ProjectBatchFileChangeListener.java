// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.virtualFileSystem.event.BatchFileChangeListener;
import org.jspecify.annotations.Nullable;

public abstract class ProjectBatchFileChangeListener implements BatchFileChangeListener {
    private final Project myProject;

    protected ProjectBatchFileChangeListener(Project project) {
        myProject = project;
    }

    public void batchChangeStarted(@Nullable String activityName) {
    }

    public void batchChangeCompleted() {
    }

    @Override
    public final void batchChangeStarted(ComponentManager project, @Nullable String activityName) {
        if (project == myProject) {
            batchChangeStarted(activityName);
        }
    }

    @Override
    public final void batchChangeCompleted(ComponentManager project) {
        if (project == myProject) {
            batchChangeCompleted();
        }
    }
}
