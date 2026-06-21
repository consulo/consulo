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
package consulo.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import org.jspecify.annotations.Nullable;

/**
 * Provides the current branch for a closed recent project, shown on the Welcome screen.
 * <p>
 * Implementations must be a stateless, blocking, off-EDT read for the given path, and must return {@code null} quickly
 * for paths they do not recognize. Caching and refresh are handled by the platform.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RecentProjectsBranchesProvider {
    @Nullable String getCurrentBranch(String projectPath);
}
