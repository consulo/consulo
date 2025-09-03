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
package consulo.diagram.impl.internal.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@ExtensionImpl
public class DiagramFileEditorProvider implements FileEditorProvider {
    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
        return file instanceof DiagramVirtualFile;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
        return new DiagramFileEditor(project, (DiagramVirtualFile) file);
    }

    @Nonnull
    @Override
    public String getEditorTypeId() {
        return "DiagramEditor";
    }
}
