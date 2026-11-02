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
package consulo.project.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.internal.ProjectBannerManager;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@TopicImpl(ComponentScope.PROJECT)
public class DumbModeBannerListener implements DumbModeListener {
    private final Project myProject;

    @Inject
    public DumbModeBannerListener(Project project) {
        myProject = project;
    }

    @Override
    public void enteredDumbMode() {
        ProjectBannerManager.getInstance(myProject).update();
    }

    @Override
    public void exitDumbMode() {
        ProjectBannerManager.getInstance(myProject).update();
    }
}
