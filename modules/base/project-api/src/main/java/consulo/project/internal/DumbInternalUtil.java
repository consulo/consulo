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
package consulo.project.internal;

import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-08-20
 */
public class DumbInternalUtil {
    /**
     * @return whether a dumb mode is in progress for the passed project or, if the argument is null, for any open project.
     * @see DumbService
     */
    public static boolean isDumbMode(@Nullable Project project) {
        if (project != null) {
            return DumbService.getInstance(project).isDumb();
        }
        for (Project proj : ProjectManager.getInstance().getOpenProjects()) {
            if (DumbService.getInstance(proj).isDumb()) {
                return true;
            }
        }
        return false;
    }
}
