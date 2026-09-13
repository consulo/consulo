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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.navigation.NavigationContextCollector;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * Collects the sand navigation context: the source file's resolution environment. The
 * platform carries it through the gesture and stamps it on the opened target editor,
 * where the banner and future variant presentation consume it.
 */
@ExtensionImpl
public class SandNavigationContextCollector implements NavigationContextCollector {
    private final Project myProject;

    @Inject
    public SandNavigationContextCollector(Project project) {
        myProject = project;
    }

    @Override
    public @Nullable Object collectContext(Editor editor, PsiFile file, int offset) {
        if (file.getLanguage() != SandLanguage.INSTANCE) {
            return null;
        }
        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        return new SandViewContext(SandFlagEnv.resolutionEnv(myProject, virtualFile), virtualFile);
    }
}
