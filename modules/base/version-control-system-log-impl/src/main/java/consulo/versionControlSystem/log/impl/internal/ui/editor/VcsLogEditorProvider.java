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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionImpl
public class VcsLogEditorProvider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(Project project, VirtualFile file) {
        return file instanceof VcsLogFile;
    }

    @RequiredUIAccess
    @Override
    public FileEditor createEditor(Project project, VirtualFile file) {
        return new VcsLogEditor(project, (VcsLogFile) file);
    }

    @Override
    public String getEditorTypeId() {
        return "VcsLogEditor";
    }

    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @RequiredUIAccess
    @Override
    public void disposeEditor(FileEditor editor) {
        if (editor instanceof VcsLogEditor logEditor) {
            logEditor.disposeLogUis();
        }
        Disposer.dispose(editor);
    }
}
