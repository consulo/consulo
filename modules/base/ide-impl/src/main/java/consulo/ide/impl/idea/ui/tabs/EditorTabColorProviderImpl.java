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
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorTabColorProvider;
import consulo.language.editor.FileColorManager;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author spleaner
 */
@ExtensionImpl
public class EditorTabColorProviderImpl implements EditorTabColorProvider, DumbAware {
    private final Provider<FileColorManager> myFileColorManager;

    @Inject
    public EditorTabColorProviderImpl(Provider<FileColorManager> fileColorManager) {
        myFileColorManager = fileColorManager;
    }

    @Override
    @Nullable
    public ColorValue getEditorTabColor(Project project, VirtualFile file) {
        FileColorManager colorManager = myFileColorManager.get();
        return colorManager.isEnabledForTabs() ? colorManager.getFileColorValue(file) : null;
    }

    @Nullable
    @Override
    public ColorValue getProjectViewColor(@Nonnull Project project, @Nonnull VirtualFile file) {
        FileColorManager colorManager = myFileColorManager.get();
        return colorManager.isEnabledForProjectView() ? colorManager.getFileColorValue(file) : null;
    }
}
