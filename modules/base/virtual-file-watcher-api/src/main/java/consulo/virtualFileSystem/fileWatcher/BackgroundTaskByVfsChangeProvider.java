/*
 * Copyright 2013-2016 consulo.io
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
package consulo.virtualFileSystem.fileWatcher;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 1:15/07.10.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class BackgroundTaskByVfsChangeProvider {
    public static abstract class ByFileType extends BackgroundTaskByVfsChangeProvider {
        private final FileType myFileType;

        public ByFileType(FileType fileType) {
            myFileType = fileType;
        }

        @Override
        public boolean validate(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
            return virtualFile.getFileType() == myFileType;
        }
    }

    public static final ExtensionPointName<BackgroundTaskByVfsChangeProvider> EP_NAME =
        ExtensionPointName.create(BackgroundTaskByVfsChangeProvider.class);

    public boolean validate(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return true;
    }

    public abstract void setDefaultParameters(
        @Nonnull Project project,
        @Nonnull VirtualFile virtualFile,
        @Nonnull BackgroundTaskByVfsParameters parameters
    );

    @Nonnull
    public abstract String getTemplateName();

    public boolean containsGeneratedFiles() {
        return false;
    }

    @Nonnull
    @RequiredReadAction
    public String[] getGeneratedFiles(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        if (!containsGeneratedFiles()) {
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile file = psiManager.findFile(virtualFile);
        if (file != null) {
            return getGeneratedFiles(file);
        }
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Nonnull
    public String[] getGeneratedFiles(@Nonnull PsiFile psiFile) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }
}
