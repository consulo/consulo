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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public class VcsDirectoryMappingsConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private final Project myProject;
    private final Consumer<Collection<AbstractVcs>> myActiveVcsesListener;

    private @Nullable VcsDirectoryConfigurationPanel myPanel;

    public VcsDirectoryMappingsConfigurable(Project project, Consumer<Collection<AbstractVcs>> activeVcsesListener) {
        myProject = project;
        myActiveVcsesListener = activeVcsesListener;
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable uiDisposable) {
        VcsDirectoryConfigurationPanel panel = new VcsDirectoryConfigurationPanel(myProject);
        panel.addVcsListener(myActiveVcsesListener::accept);
        myPanel = panel;
        return panel.createComponent(uiDisposable);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return myPanel != null && myPanel.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myPanel != null) {
            myPanel.apply();
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        if (myPanel != null) {
            myPanel.reset();
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myPanel != null) {
            myPanel.disposeUIResources();
            myPanel = null;
        }
    }

    @Override
    public LocalizeValue getDisplayName() {
        return VcsLocalize.settingsDirectoryMappingsConfigurableName();
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.DirectoryMappings";
    }
}
