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

import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

/**
 * Shows a log in an editor tab.
 */
public class VcsLogEditor extends UserDataHolderBase implements FileEditor {
    private final Project myProject;
    private final VcsLogFile myFile;
    private @Nullable Component myComponent;

    public VcsLogEditor(Project project, VcsLogFile file) {
        myProject = project;
        myFile = file;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        Component component = myComponent;
        if (component == null) {
            component = myFile.createMainComponent(myProject);
            myComponent = component;
        }
        return component;
    }

    @Override
    public String getName() {
        return VersionControlSystemLogLocalize.editorName().get();
    }

    @Override
    public VcsLogFile getFile() {
        return myFile;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @RequiredUIAccess
    public void disposeLogUis() {
        myFile.disposeLogUis();
    }

    @Override
    public void dispose() {
        myComponent = null;
    }
}
