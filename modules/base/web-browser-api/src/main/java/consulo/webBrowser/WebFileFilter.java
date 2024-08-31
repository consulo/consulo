/*
 * Copyright 2013-2022 consulo.io
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
package consulo.webBrowser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24-Apr-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface WebFileFilter {
    ExtensionPointName<WebFileFilter> EP_NAME = ExtensionPointName.create(WebFileFilter.class);

    static boolean isFileAllowed(@Nonnull PsiFile file) {
        return isFileAllowed(file.getProject(), file.getViewProvider().getVirtualFile());
    }

    static boolean isFileAllowed(@Nonnull Project project, @Nonnull VirtualFile file) {
        return EP_NAME.computeSafeIfAny(Application.get(), it -> it.isWebFile(project, file) ? it : null) != null;
    }

    boolean isWebFile(@Nonnull Project project, @Nonnull VirtualFile file);
}
