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
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusProvider;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * A {@link FileStatusProvider} that marks files as {@link FileStatus#IGNORED} when any
 * registered {@link IgnoredFileProvider} considers them ignored (e.g. workspace.xml, shelf dir).
 * <p>
 * This runs before {@link consulo.versionControlSystem.impl.internal.VcsFileStatusProvider} and
 * therefore works independently of whether a VCS is configured for the file.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class IgnoredFileStatusProvider implements FileStatusProvider {
    private final Project myProject;

    @Inject
    public IgnoredFileStatusProvider(Project project) {
        myProject = project;
    }

    @Nullable
    @Override
    public FileStatus getFileStatus(VirtualFile virtualFile) {
        if (!virtualFile.isInLocalFileSystem()) {
            return null;
        }
        var fp = VcsUtil.getFilePath(virtualFile);
        var ignored = myProject.getExtensionPoint(IgnoredFileProvider.class)
                               .findFirstSafe(p -> p.isIgnoredFilePath(fp));
        return ignored != null ? FileStatus.IGNORED : null;
    }
}
