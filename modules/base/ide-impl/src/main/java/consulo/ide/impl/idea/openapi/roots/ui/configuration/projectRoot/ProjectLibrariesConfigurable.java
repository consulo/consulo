/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.content.library.LibraryTablePresentation;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ProjectLibrariesConfigurable extends BaseLibrariesConfigurable
    implements ConfigurableWeight, ProjectConfigurable, NonDefaultProjectConfigurable {
    public static final String ID = "project.libraries";

    @Inject
    public ProjectLibrariesConfigurable(Project project) {
        super(project, LibraryTablesRegistrar.PROJECT_LEVEL);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.librariesDisplayName();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LibraryTableModifiableModelProvider getModelProvider() {
        return getLibrariesConfigurator().getProjectLibrariesProvider();
    }

    @Override
    public LibraryTablePresentation getLibraryTablePresentation() {
        return LibraryTablesRegistrar.getInstance().getLibraryTable(myProject).getPresentation();
    }

    @Override
    protected LocalizeValue getAddText() {
        return ProjectLocalize.addNewProjectLibraryText();
    }

    @Override
    public int getConfigurableWeight() {
        return ProjectConfigurableWeights.LIBRARIES;
    }
}
