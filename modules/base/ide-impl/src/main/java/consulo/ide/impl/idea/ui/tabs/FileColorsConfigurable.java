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

package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.FileColorManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;

/**
 * @author spleaner
 */
@ExtensionImpl
public class FileColorsConfigurable implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable {
    private final Project myProject;
    private FileColorsConfigurablePanel myPanel;

    @Inject
    public FileColorsConfigurable(@Nonnull Project project) {
        myProject = project;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("File Colors");
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(Disposable uiDisposable) {
        if (myPanel == null) {
            FileColorsConfigurablePanel panel = new FileColorsConfigurablePanel(myProject, (FileColorManagerImpl) FileColorManager.getInstance(myProject));
            myPanel = panel;
            Disposer.register(uiDisposable, panel);
        }

        return myPanel;
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
        myPanel = null;
    }

    @Nonnull
    @Override
    public String getId() {
        return "fileColors";
    }
}
