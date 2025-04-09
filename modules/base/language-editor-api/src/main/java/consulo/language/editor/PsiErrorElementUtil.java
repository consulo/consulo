/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public class PsiErrorElementUtil {

    private static final Key<CachedValue<Boolean>> CONTAINS_ERROR_ELEMENT = Key.create("CONTAINS_ERROR_ELEMENT");

    private PsiErrorElementUtil() {
    }

    public static boolean hasErrors(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return Application.get().runReadAction((Supplier<Boolean>)() -> {
            if (project.isDisposed() || !virtualFile.isValid()) {
                return false;
            }
            PsiManager psiManager = PsiManager.getInstance(project);
            PsiFile psiFile = psiManager.findFile(virtualFile);
            return psiFile != null && hasErrors(psiFile);
        });
    }

    private static boolean hasErrors(@Nonnull PsiFile psiFile) {
        CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(psiFile.getProject());
        return cachedValuesManager.getCachedValue(psiFile,
            CONTAINS_ERROR_ELEMENT,
            () -> {
                boolean error = hasErrorElements(psiFile);
                return CachedValueProvider.Result.create(error, psiFile);
            },
            false
        );
    }

    @RequiredReadAction
    private static boolean hasErrorElements(@Nonnull PsiElement element) {
        if (element instanceof PsiErrorElement errorElement) {
            for (HighlightErrorFilter errorFilter : HighlightErrorFilter.EP_NAME.getExtensionList(element.getProject())) {
                if (!errorFilter.shouldHighlightErrorElement(errorElement)) {
                    return false;
                }
            }
            return true;
        }
        for (PsiElement child : element.getChildren()) {
            if (hasErrorElements(child)) {
                return true;
            }
        }
        return false;
    }
}
