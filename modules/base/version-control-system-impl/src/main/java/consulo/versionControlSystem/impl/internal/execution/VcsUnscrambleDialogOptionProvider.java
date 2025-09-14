/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.impl.internal.execution;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.UnnamedConfigurable;
import consulo.execution.unscramble.UnscrambleDialogOptionProvider;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.impl.internal.configurable.VcsContentAnnotationConfigurable;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2025-09-14
 */
@ExtensionImpl
public class VcsUnscrambleDialogOptionProvider implements UnscrambleDialogOptionProvider {
    private final Project myProject;

    @Inject
    public VcsUnscrambleDialogOptionProvider(Project project) {
        myProject = project;
    }

    @Nullable
    @Override
    public UnnamedConfigurable createConfigurable() {
        if (ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) {
            return new VcsContentAnnotationConfigurable(myProject);
        }
        return null;
    }
}
