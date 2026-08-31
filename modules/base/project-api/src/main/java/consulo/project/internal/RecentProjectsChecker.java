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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Background checker for recent projects shown on the Welcome screen.
 * <p>
 * Owns the periodic, off-EDT checks for each recent project path: whether the path is still available, and what its
 * current VCS branch is. Checking runs only while at least one callback is registered (i.e. a Welcome screen is
 * showing) and the application is active; the worker thread is stopped when the last callback is removed.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RecentProjectsChecker {
    static RecentProjectsChecker getInstance() {
        return Application.get().getInstance(RecentProjectsChecker.class);
    }

    /**
     * @return {@code false} if the path is known to be unavailable (e.g. on a disconnected drive), {@code true} otherwise.
     */
    boolean isValid(String path);

    /**
     * @return the last known VCS branch for the given path, or {@code null} if none is known.
     */
    @Nullable String getBranch(String path);

    /**
     * Registers a callback fired (on the EDT) whenever the validity or branch of any of the given paths changes.
     * Registering the first callback starts the background checks.
     */
    void addCallback(Runnable callback, Collection<String> paths);

    /**
     * Removes a previously registered callback. Removing the last callback stops the background checks.
     */
    void removeCallback(Runnable callback);
}
