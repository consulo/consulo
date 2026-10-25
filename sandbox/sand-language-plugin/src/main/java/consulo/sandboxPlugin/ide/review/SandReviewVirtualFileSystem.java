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
package consulo.sandboxPlugin.ide.review;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.editor.ComplexPathVirtualFileSystem;
import consulo.versionControlSystem.editor.GsonComplexPathSerializer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandReviewVirtualFileSystem extends ComplexPathVirtualFileSystem<SandReviewFilePath> {
    public static final String PROTOCOL = "sand-review";

    public static SandReviewVirtualFileSystem getInstance() {
        return (SandReviewVirtualFileSystem) Objects.requireNonNull(VirtualFileManager.getInstance().getFileSystem(PROTOCOL));
    }

    public SandReviewVirtualFileSystem() {
        super(new GsonComplexPathSerializer<>(SandReviewFilePath.class));
    }

    @Override
    protected VirtualFile findOrCreateFile(Project project, SandReviewFilePath path) {
        return project.getInstance(SandReviewService.class).getDiffFile(path.reviewNumber(), path.sessionId());
    }

    public String getPath(Project project, String sessionId, String reviewNumber) {
        return getPath(new SandReviewFilePath(sessionId, project.getLocationHash(), reviewNumber));
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }
}
