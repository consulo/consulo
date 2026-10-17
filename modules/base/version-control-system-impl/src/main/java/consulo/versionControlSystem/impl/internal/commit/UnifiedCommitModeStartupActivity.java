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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.versionControlSystem.VcsInitObject;
import consulo.versionControlSystem.VcsStartupActivity;
import consulo.versionControlSystem.commit.CommitModeManager;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@ExtensionImpl
public class UnifiedCommitModeStartupActivity implements VcsStartupActivity {
    private static final int ORDER_AFTER_MAPPINGS = 50;

    private final Project myProject;
    private final Application myApplication;

    @Inject
    public UnifiedCommitModeStartupActivity(Project project, Application application) {
        myProject = project;
        myApplication = application;
    }

    @Override
    public void runActivity() {
        if (!(CommitModeManager.getInstance(myProject) instanceof UnifiedCommitModeManager manager)) {
            return;
        }

        myApplication.invokeLater(manager::initialize, ModalityState.nonModal(), myProject.getDisposed());
    }

    @Override
    public int getOrder() {
        return VcsInitObject.MAPPINGS.getOrder() + ORDER_AFTER_MAPPINGS;
    }
}
