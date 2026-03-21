/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.service.project.manage;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Manages task activation hooks across build phases.
 * Tasks can be registered to run before/after compilation, rebuild, and sync operations.
 *
 * @author Vladislav.Soroka
 */
public class ExternalSystemTaskActivator {
    /**
     * Build phases at which an external system task can be activated.
     */
    public enum Phase {
        BEFORE_RUN("before run"),
        BEFORE_SYNC("before sync"),
        AFTER_SYNC("after sync"),
        BEFORE_COMPILE("before compile"),
        AFTER_COMPILE("after compile"),
        BEFORE_REBUILD("before rebuild"),
        AFTER_REBUILD("after rebuild");

        private final String myDisplayName;

        Phase(String displayName) {
            myDisplayName = displayName;
        }

        public String getDisplayName() {
            return myDisplayName;
        }
    }

    private final Project myProject;

    public ExternalSystemTaskActivator(Project project) {
        myProject = project;
    }

    /**
     * Returns a human-readable description of the phases this task is activated in,
     * e.g. "before compile, after sync", or null if not activated in any phase.
     */
    @Nullable
    public String getDescription(ProjectSystemId systemId,
                                 String projectPath,
                                 String taskName) {
        return null; // TODO: read from persisted activation state
    }

    /**
     * Add a listener that will be notified when task activation configuration changes.
     */
    public void addListener(Runnable listener, Object parentDisposable) {
        // TODO: notify on state changes
    }
}
