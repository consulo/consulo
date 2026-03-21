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

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.project.Project;

import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Manages keyboard shortcuts for external system tasks and run configurations.
 * Provides human-readable shortcut descriptions for display in the task tree.
 *
 * @author Vladislav.Soroka
 */
public class ExternalSystemShortcutsManager {
    private final Project myProject;
    private Consumer<Void> myListener;

    public ExternalSystemShortcutsManager(Project project) {
        myProject = project;
    }

    /**
     * Returns a human-readable description of the shortcut bound to this task,
     * e.g. "Ctrl+F5", or null if no shortcut is assigned.
     */
    @Nullable
    public String getDescription(String projectPath, String taskName) {
        return null; // TODO: integrate with keymap
    }

    /**
     * Returns a human-readable description of the shortcut bound to this run configuration,
     * or null if no shortcut is assigned.
     */
    @Nullable
    public String getDescription(RunnerAndConfigurationSettings settings) {
        return null; // TODO: integrate with keymap
    }

    /**
     * Returns true if there is a keyboard shortcut bound to the given task.
     */
    public boolean hasShortcuts(String projectPath, String taskName) {
        return false;
    }

    /**
     * Add a listener that will be notified when shortcut assignments change.
     */
    public void addListener(Runnable listener, Object parentDisposable) {
        // TODO: subscribe to keymap changes
    }
}
