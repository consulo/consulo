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
package consulo.versionControlSystem.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsProjectLog {
    static VcsProjectLog getInstance(Project project) {
        return project.getInstance(VcsProjectLog.class);
    }

    /**
     * Opens a new log tab in the VCS tool window with the given filters applied.
     */
    @RequiredUIAccess
    @Nullable
    VcsLogUi openLogTab(VcsLogFilterCollection filters);
}
