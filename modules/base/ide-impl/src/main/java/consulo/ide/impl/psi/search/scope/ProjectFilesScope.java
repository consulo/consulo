/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.psi.search.scope;

import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectFilesScope extends NamedScope {
    public static final String ID = "Project Files";

    @Deprecated
    public static final String NAME = ID;

    public ProjectFilesScope() {
        super(ID, LocalizeValue.localizeTODO("Project Files"), new AbstractPackageSet("ProjectFiles") {
            @Override
            public boolean contains(VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
                if (file == null) {
                    return false;
                }
                ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                return project.isInitialized() && !fileIndex.isExcluded(file) && fileIndex.getContentRootForFile(file) != null;
            }
        });
    }
}
