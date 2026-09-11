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
package consulo.language.editor.impl.internal.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.virtualFileSystem.VirtualFile;

/**
 * View options belong to the editors a navigation opened: once the last editor of a file is gone the file goes back to
 * its module-settings variant, so a later plain open shows no leftover context.
 */
@TopicImpl(ComponentScope.PROJECT)
public class ViewOptionsFileEditorListener implements FileEditorManagerListener {
    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
        if (source.getAllEditors(file).length == 0 && ModuleAwareIndexOptions.getViewOptions(file) != null) {
            ModuleAwareIndexOptions.setViewOptions(source.getProject(), file, null);
        }
    }
}
