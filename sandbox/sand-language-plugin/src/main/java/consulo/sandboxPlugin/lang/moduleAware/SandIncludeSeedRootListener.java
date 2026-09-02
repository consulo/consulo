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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import jakarta.inject.Inject;

/**
 * Module flags are part of every walk's entry environment — a roots change re-derives the
 * include seeds.
 */
@TopicImpl(ComponentScope.PROJECT)
final class SandIncludeSeedRootListener implements ModuleRootListener {
    private final Project myProject;

    @Inject
    SandIncludeSeedRootListener(Project project) {
        myProject = project;
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
        SandSeedEnv.scheduleRecompute(myProject);
    }
}
