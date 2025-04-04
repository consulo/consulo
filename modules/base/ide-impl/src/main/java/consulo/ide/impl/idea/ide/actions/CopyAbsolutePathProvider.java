/*
 * Copyright 2013-2021 consulo.io
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

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nullable;

// from kotlin
public class CopyAbsolutePathProvider extends DumbAwareCopyPathProvider {
    @Nullable
    @Override
    public String getPathToElement(Project project, @Nullable VirtualFile virtualFile, @Nullable Editor editor) {
        return ObjectUtil.doIfNotNull(virtualFile, VirtualFile::getPresentableUrl);
    }
}
