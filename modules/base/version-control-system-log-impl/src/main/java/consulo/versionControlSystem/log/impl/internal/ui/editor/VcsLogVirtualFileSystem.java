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
import consulo.versionControlSystem.editor.ComplexPathVirtualFileSystem;
import consulo.versionControlSystem.editor.GsonComplexPathSerializer;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionImpl
public class VcsLogVirtualFileSystem extends ComplexPathVirtualFileSystem<VcsLogComplexPath> {
    public static final String PROTOCOL = "vcs-log";

    public static VcsLogVirtualFileSystem getInstance() {
        return (VcsLogVirtualFileSystem) Objects.requireNonNull(VirtualFileManager.getInstance().getFileSystem(PROTOCOL));
    }

    public VcsLogVirtualFileSystem() {
        super(new GsonComplexPathSerializer<>(VcsLogComplexPath.class));
    }

    @Override
    protected VirtualFile findOrCreateFile(Project project, VcsLogComplexPath path) {
        return createVcsLogFile(path, null);
    }

    public VirtualFile createVcsLogFile(Project project, String tabId, @Nullable VcsLogFilterCollection filters) {
        return createVcsLogFile(new VcsLogComplexPath(project.getLocationHash(), project.getLocationHash(), tabId), filters);
    }

    private DefaultVcsLogFile createVcsLogFile(VcsLogComplexPath pathId, @Nullable VcsLogFilterCollection filters) {
        return new DefaultVcsLogFile(this, pathId, filters);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }
}
