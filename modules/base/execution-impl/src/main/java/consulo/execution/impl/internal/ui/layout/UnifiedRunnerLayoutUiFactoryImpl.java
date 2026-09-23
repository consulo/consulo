/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.impl.internal.ui.layout;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.execution.ui.layout.RunnerLayoutUiFactory;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedRunnerLayoutUiFactoryImpl implements RunnerLayoutUiFactory {
    private final Project myProject;

    @Inject
    public UnifiedRunnerLayoutUiFactoryImpl(Project project) {
        myProject = project;
    }

    @Override
    public RunnerLayoutUi create(String runnerId, String runnerTitle, String sessionName, Disposable parent) {
        return new UnifiedRunnerLayoutUiImpl(myProject, parent, runnerId, runnerTitle, sessionName);
    }
}
