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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.internal.SilentChangeVetoer;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Collections;

/**
 * @author VISTALL
 * @since 2025-08-09
 */
@ExtensionImpl
public class VcsSilentChangeVetoer implements SilentChangeVetoer {
    private final Provider<ProjectLevelVcsManager> myProjectLevelVcsManager;
    private final Provider<VcsDirtyScopeManager> myVcsDirtyScopeManager;
    private final Provider<FileStatusManager> myFileStatusManager;

    @Inject
    public VcsSilentChangeVetoer(Provider<ProjectLevelVcsManager> projectLevelVcsManager, Provider<VcsDirtyScopeManager> vcsDirtyScopeManager, Provider<FileStatusManager> fileStatusManager) {
        myProjectLevelVcsManager = projectLevelVcsManager;
        myVcsDirtyScopeManager = vcsDirtyScopeManager;
        myFileStatusManager = fileStatusManager;
    }

    @Nonnull
    @Override
    public ThreeState canChangeFileSilently(@Nonnull VirtualFile virtualFile) {
        AbstractVcs activeVcs = myProjectLevelVcsManager.get().getVcsFor(virtualFile);
        if (activeVcs == null) {
            return ThreeState.UNSURE;
        }

        FilePath path = VcsUtil.getFilePath(virtualFile);
        boolean vcsIsThinking = !myVcsDirtyScopeManager.get().whatFilesDirty(Collections.singletonList(path)).isEmpty();
        if (vcsIsThinking) {
            return ThreeState.UNSURE; // do not modify file which is in the process of updating
        }

        FileStatus status = myFileStatusManager.get().getStatus(virtualFile);
        if (status == FileStatus.UNKNOWN) {
            return ThreeState.UNSURE;
        }
        return status == FileStatus.MODIFIED || status == FileStatus.ADDED ? ThreeState.YES : ThreeState.NO;
    }
}
