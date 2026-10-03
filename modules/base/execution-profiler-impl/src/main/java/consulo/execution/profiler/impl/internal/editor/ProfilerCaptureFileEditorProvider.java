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
package consulo.execution.profiler.impl.internal.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.profiler.impl.internal.view.ProfilerCaptureViewFactory;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class ProfilerCaptureFileEditorProvider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(Project project, VirtualFile file) {
        return file instanceof ProfilerCaptureVirtualFile;
    }

    @RequiredUIAccess
    @Override
    public FileEditor createEditor(Project project, VirtualFile file) {
        return new ProfilerCaptureFileEditor((ProfilerCaptureVirtualFile) file, project.getInstance(ProfilerCaptureViewFactory.class));
    }

    @Override
    public String getEditorTypeId() {
        return "ProfilerCaptureEditor";
    }

    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
