/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.ui.image.Image;

import jakarta.annotation.Nullable;

public class OpenProjectFileChooserDescriptor extends FileChooserDescriptor {
    public OpenProjectFileChooserDescriptor(boolean chooseFiles) {
        super(chooseFiles, true, chooseFiles, chooseFiles, false, false);
    }

    @RequiredUIAccess
    @Override
    public boolean isFileSelectable(VirtualFile file) {
        return file != null && (isProjectDirectory(file) || canOpen(file));
    }

    @Override
    public Image getIcon(VirtualFile file) {
        if (isProjectDirectory(file)) {
            return dressIcon(file, Application.get().getIcon());
        }
        Image icon = getProcessorIcon(file);
        return icon != null ? dressIcon(file, icon) : super.getIcon(file);
    }

    @Nullable
    private Image getProcessorIcon(VirtualFile virtualFile) {
        ProjectOpenProcessor provider = ProjectOpenProcessors.getInstance().findProcessor(VfsUtilCore.virtualToIoFile(virtualFile));
        if (provider != null) {
            return provider.getIcon(virtualFile);
        }
        return null;
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!showHiddenFiles && FileElement.isFileHidden(file)) {
            return false;
        }
        return canOpen(file) || super.isFileVisible(file, showHiddenFiles) && file.isDirectory();
    }

    public static boolean canOpen(VirtualFile file) {
        return ProjectOpenProcessors.getInstance().findProcessor(VfsUtilCore.virtualToIoFile(file)) != null;
    }

    private static boolean isProjectDirectory(VirtualFile virtualFile) {
        // the root directory of any drive is never an Consulo project
        if (virtualFile.getParent() == null) {
            return false;
        }
        // NOTE: For performance reasons, it's very important not to iterate through all of the children here.
        return virtualFile.isDirectory() && virtualFile.isValid() && virtualFile.findChild(Project.DIRECTORY_STORE_FOLDER) != null;
    }
}
