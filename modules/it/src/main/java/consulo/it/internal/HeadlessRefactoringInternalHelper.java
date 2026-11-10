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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.UsageTarget;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceImpl
@Singleton
public class HeadlessRefactoringInternalHelper implements RefactoringInternalHelper {
    @Override
    public UsageTarget createPsiElement2UsageTargetAdapter(PsiElement element) {
        throw new UnsupportedOperationException("usage targets are not available headless");
    }

    @Override
    public @Nullable PsiDirectory chooseDirectory(
        PsiDirectory[] targetDirectories,
        @Nullable PsiDirectory initialDirectory,
        Project project,
        Map<PsiDirectory, String> relativePathsToCreate
    ) {
        if (initialDirectory != null) {
            return initialDirectory;
        }
        return targetDirectories.length == 1 ? targetDirectories[0] : null;
    }
}
