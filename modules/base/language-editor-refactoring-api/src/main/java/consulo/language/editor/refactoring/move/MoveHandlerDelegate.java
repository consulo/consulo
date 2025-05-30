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

package consulo.language.editor.refactoring.move;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.project.Project;

import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class MoveHandlerDelegate {
    public static final ExtensionPointName<MoveHandlerDelegate> EP_NAME = ExtensionPointName.create(MoveHandlerDelegate.class);

    public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
        return targetContainer == null || isValidTarget(targetContainer, elements);
    }

    public boolean canMove(DataContext dataContext) {
        return false;
    }

    public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
        return false;
    }

    public void doMove(
        Project project,
        PsiElement[] elements,
        @Nullable PsiElement targetContainer,
        @Nullable MoveCallback callback
    ) {
    }

    public PsiElement adjustTargetForMove(DataContext dataContext, PsiElement targetContainer) {
        return targetContainer;
    }

    @Nullable
    public PsiElement[] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement) {
        return sourceElements;
    }

    public boolean tryToMove(
        PsiElement element,
        Project project,
        DataContext dataContext,
        @Nullable PsiReference reference,
        Editor editor
    ) {
        return false;
    }

    public void collectFilesOrDirsFromContext(DataContext dataContext, Set<PsiElement> filesOrDirs) {
    }

    public boolean isMoveRedundant(PsiElement source, PsiElement target) {
        return false;
    }
}
