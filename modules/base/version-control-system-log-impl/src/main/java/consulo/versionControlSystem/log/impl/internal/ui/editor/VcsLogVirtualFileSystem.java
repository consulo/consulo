/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal.ui.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Files of the log tabs opened in editors, addressed by the project and the id of the tab.
 */
@ExtensionImpl
public class VcsLogVirtualFileSystem extends BaseVirtualFileSystem {
    public static final String PROTOCOL = "vcs-log";

    private static final char SEPARATOR = '/';

    public static VcsLogVirtualFileSystem getInstance() {
        return (VcsLogVirtualFileSystem) Objects.requireNonNull(VirtualFileManager.getInstance().getFileSystem(PROTOCOL));
    }

    public VirtualFile createVcsLogFile(Project project, String tabId, @Nullable VcsLogFilterCollection filters) {
        return new DefaultVcsLogFile(this, project.getLocationHash() + SEPARATOR + tabId, tabId, filters);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        int index = path.indexOf(SEPARATOR);
        if (index <= 0 || index == path.length() - 1) {
            return null;
        }

        String projectHash = path.substring(0, index);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (projectHash.equals(project.getLocationHash())) {
                return new DefaultVcsLogFile(this, path, path.substring(index + 1), null);
            }
        }
        return null;
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public String extractPresentableUrl(String path) {
        return findFileByPath(path) instanceof DefaultVcsLogFile file ? file.getTabName() : path;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }
}
