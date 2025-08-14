/*
 * Copyright 2013-2016 consulo.io
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

import consulo.application.AllIcons;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.localize.IdeLocalize;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 18:03/17.06.13
 */
public class ResourcesScope extends NamedScope {
    public static final String ID = "Resources";

    @Deprecated
    public static final String NAME = ID;

    public ResourcesScope() {
        super(ID, IdeLocalize.predefinedScopeResourcesName(), AllIcons.Modules.ResourcesRoot, new AbstractPackageSet("rsc:*..*") {
            @Override
            public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
                ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
                return file != null && index.isInResource(file);
            }
        });
    }
}
