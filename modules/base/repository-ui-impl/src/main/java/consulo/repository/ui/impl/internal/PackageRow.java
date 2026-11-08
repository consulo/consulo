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
package consulo.repository.ui.impl.internal;

import consulo.repository.ui.InstalledPackage;
import consulo.repository.ui.RepoPackage;
import org.jspecify.annotations.Nullable;

/**
 * A package as a list shows it: installed, in the repository, or both.
 *
 * @param latestVersion   the latest version in the repository, if known
 * @param updateAvailable whether the latest version is newer than the installed one
 */
public record PackageRow(String name,
                         @Nullable InstalledPackage installed,
                         @Nullable RepoPackage repoPackage,
                         @Nullable String latestVersion,
                         boolean updateAvailable) {
    public @Nullable String installedVersion() {
        return installed == null ? null : installed.getVersion();
    }

    public RepoPackage toRepoPackage() {
        return repoPackage != null ? repoPackage : new RepoPackage(name, null, latestVersion);
    }
}
