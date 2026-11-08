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
package consulo.repository.ui.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionImpl
public class PackagesFileEditorProvider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(Project project, VirtualFile file) {
        return file instanceof PackagesVirtualFile;
    }

    @RequiredUIAccess
    @Override
    public FileEditor createEditor(Project project, VirtualFile file) {
        return new PackagesFileEditor(project, (PackagesVirtualFile) file);
    }

    @Override
    public String getEditorTypeId() {
        return "PackagesEditor";
    }
}
