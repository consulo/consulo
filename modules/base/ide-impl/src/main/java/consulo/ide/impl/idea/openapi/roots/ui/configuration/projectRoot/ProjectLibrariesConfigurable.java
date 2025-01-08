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
import consulo.configurable.internal.FullContentConfigurable;
import consulo.content.library.LibraryTablePresentation;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;

@ExtensionImpl
public class ProjectLibrariesConfigurable extends BaseLibrariesConfigurable implements ConfigurableWeight,
    ProjectConfigurable,
    NonDefaultProjectConfigurable,
    FullContentConfigurable {
    public static final String ID = "project.libraries";

    @Inject
    public ProjectLibrariesConfigurable(final Project project) {
        super(project);
        myLevel = LibraryTablesRegistrar.PROJECT_LEVEL;
    }

    @Override
    public void setBannerComponent(JComponent bannerComponent) {
        myNorthPanel.add(bannerComponent, BorderLayout.NORTH);
    }

    @Override
    protected String getComponentStateKey() {
        return "ProjectLibrariesConfigurable.UI";
    }

    @Override
    @Nls
    public String getDisplayName() {
        return "Libraries";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    @Nonnull
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
    protected String getAddText() {
        return ProjectBundle.message("add.new.project.library.text");
    }

    @Override
    public int getConfigurableWeight() {
        return ProjectConfigurableWeights.LIBRARIES;
    }
}
