// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.distributed.repository;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.container.boot.ContainerPathManager;
import consulo.project.Project;
import consulo.util.io.PathUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsRememberedInputs;
import org.jspecify.annotations.Nullable;

/**
 * Provides default paths for clone repository functionality.
 * Default implementation suggests to clone a project to the `[project dir]/[last url component]`.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ClonePathProvider {
    public static String defaultParentDirectoryPath(Project project, DvcsRememberedInputs rememberedInputs) {
        String directoryPath = project.getApplication().getExtensionPoint(ClonePathProvider.class)
            .computeSafeIfAny(provider -> StringUtil.nullize(provider.getParentDirectoryPath(project, rememberedInputs)));
        if (directoryPath != null) {
            return directoryPath;
        }

        String parentDirectory = rememberedInputs.getCloneParentDir();
        return StringUtil.isEmptyOrSpaces(parentDirectory) ? ContainerPathManager.get().getDocumentsDir().getPath() : parentDirectory;
    }

    public static String relativeDirectoryPathForVcsUrl(Project project, String vcsUrl) {
        String directoryPath = project.getApplication().getExtensionPoint(ClonePathProvider.class)
            .computeSafeIfAny(provider -> StringUtil.nullize(provider.getRelativeDirectoryPathForVcsUrl(project, vcsUrl)));
        if (directoryPath != null) {
            return directoryPath;
        }
        String encoded = PathUtil.getFileName(vcsUrl);
        try {
            return URLUtil.decode(encoded);
        }
        catch (Exception e) {
            return encoded;
        }
    }

    public abstract @Nullable String getParentDirectoryPath(Project project, DvcsRememberedInputs rememberedInputs);

    public abstract @Nullable String getRelativeDirectoryPathForVcsUrl(Project project, String vcsUrl);
}
