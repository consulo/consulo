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
package consulo.ai.impl.internal.chat;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

/**
 * The chats as files of the editor tabs - a path is the project and the chat, so the tabs of a project come back when
 * it is opened again.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class AIChatVirtualFileSystem extends BaseVirtualFileSystem implements NonPhysicalFileSystem {
    public static final String PROTOCOL = "ai-chat";

    private final Provider<ProjectManager> myProjectManager;

    @Inject
    public AIChatVirtualFileSystem(Provider<ProjectManager> projectManager) {
        myProjectManager = projectManager;
    }

    static String path(Project project, String chatId) {
        return project.getLocationHash() + "/" + chatId;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        int separator = path.indexOf('/');
        if (separator <= 0 || separator == path.length() - 1) {
            return null;
        }
        Project project = myProjectManager.get().findOpenProjectByHash(path.substring(0, separator));
        if (project == null || project.isDisposed()) {
            return null;
        }
        return project.getInstance(AIChatView.class).findFile(path.substring(separator + 1));
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public String extractPresentableUrl(String path) {
        VirtualFile file = findFileByPath(path);
        return file == null ? path : file.getPresentableName();
    }

    @Override
    public void refresh(boolean asynchronous) {
    }
}
