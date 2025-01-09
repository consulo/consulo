/*
 * Copyright 2013-2025 consulo.io
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
package consulo.configuration.editor;

import consulo.configuration.editor.internal.ConfigurationEditorFileReference;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeListener;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public abstract class ConfigurationFileEditor extends UserDataHolderBase implements FileEditor {
    protected final VirtualFile myVirtualFile;
    protected final Project myProject;

    protected ConfigurationFileEditor(Project project, VirtualFile virtualFile) {
        myVirtualFile = virtualFile;
        myProject = project;
    }

    @Nonnull
    @Override
    public String getName() {
        ConfigurationEditorFileReference ref = (ConfigurationEditorFileReference) myVirtualFile.getFileType();
        return ref.getProvider().getName().get();
    }

    @Override
    public VirtualFile getFile() {
        return myVirtualFile;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }
}
