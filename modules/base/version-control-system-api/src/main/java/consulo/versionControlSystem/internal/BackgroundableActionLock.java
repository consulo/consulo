/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.internal;

import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;

public class BackgroundableActionLock {
    
    private final Project myProject;
    
    private final Object[] myKeys;

    BackgroundableActionLock(Project project, Object[] keys) {
        myProject = project;
        myKeys = keys;
    }

    public boolean isLocked() {
        return isLocked(myProject, myKeys);
    }

    public void lock() {
        lock(myProject, myKeys);
    }

    public void unlock() {
        unlock(myProject, myKeys);
    }

    
    public static BackgroundableActionLock getLock(Project project, Object... keys) {
        return new BackgroundableActionLock(project, keys);
    }

    public static boolean isLocked(Project project, Object... keys) {
        return getManager(project).isBackgroundTaskRunning(keys);
    }

    public static void lock(Project project, Object... keys) {
        getManager(project).startBackgroundTask(keys);
    }

    public static void unlock(Project project, Object... keys) {
        getManager(project).stopBackgroundTask(keys);
    }

    
    private static ProjectLevelVcsManagerEx getManager(Project project) {
        return (ProjectLevelVcsManagerEx) ProjectLevelVcsManager.getInstance(project);
    }
}
