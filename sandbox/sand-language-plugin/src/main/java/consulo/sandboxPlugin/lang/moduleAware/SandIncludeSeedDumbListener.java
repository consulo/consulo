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
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import jakarta.inject.Inject;

/**
 * Every dumb-to-smart transition re-derives the include seeds: the initial indexing pass is
 * the first deterministic moment the include index is queryable, and each seed-driven reindex
 * converges to a fixed point through the same hook.
 */
@TopicImpl(ComponentScope.PROJECT)
final class SandIncludeSeedDumbListener implements DumbModeListener {
    private final Project myProject;

    @Inject
    SandIncludeSeedDumbListener(Project project) {
        myProject = project;
    }

    @Override
    public void exitDumbMode() {
        SandSeedEnv.scheduleRecompute(myProject);
    }
}
