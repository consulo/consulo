/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project.impl.internal.store;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.store.internal.IComponentStore;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.project.impl.internal.ProjectImpl;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @see ProjectImpl#getStateStore()
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface IProjectStore extends IComponentStore {
    void setProjectDir(VirtualFile projectDir);

    void setProjectFilePathNoUI(String filePath);

    void setProjectFilePath(String filePath);

    @Nullable VirtualFile getProjectBaseDir();

    @Nullable String getProjectBasePath();

    String getProjectName();

    TrackingPathMacroSubstitutor[] getSubstitutors();

    @Nullable String getPresentableUrl();

    @Nullable VirtualFile getProjectFile();

    @Nullable VirtualFile getWorkspaceFile();

    void loadProjectFromTemplate(ProjectImpl project);

    String getProjectFilePath();
}
