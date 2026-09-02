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

import consulo.virtualFileSystem.VirtualFile;

import java.util.Set;

/**
 * The context a file is viewed under: the flag environment of the navigation origin.
 * Collected by {@code SandNavigationContextCollector}, carried by the platform's
 * {@code NavigationContexts} and read back from the target editor's
 * {@code NavigationContexts.NAVIGATION_CONTEXTS} user data by presentation layers
 * (banner, future inactive-branch dimming).
 */
public record SandViewContext(Set<String> environment, VirtualFile navigationSource) {
}
