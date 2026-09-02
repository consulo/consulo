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
package consulo.sandboxPlugin.ide.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.moduleAware.SandViewEnv;
import consulo.sandboxPlugin.lang.psi.SandConditions;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Dims a conditional-block segment whose guard does not hold in the file's current view
 * environment — the CLion-style inactive-branch rendering. Text and PSI stay untouched:
 * the branch is fully parsed and indexed, only its presentation goes gray; arriving with a
 * different navigation context is a re-highlight, never a re-parse.
 */
public class SandInactiveBranchAnnotator implements Annotator {
    private final Project myProject;

    SandInactiveBranchAnnotator(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (element instanceof PsiWhiteSpace) {
            return;
        }
        PsiElement parent = element.getParent();
        ASTNode parentNode = parent == null ? null : parent.getNode();
        if (parentNode == null || parentNode.getElementType() != SandElements.CONDITIONAL_BLOCK) {
            return;
        }
        ASTNode node = element.getNode();
        if (node == null || isDirective(node.getElementType())) {
            return;
        }

        String condition = SandConditions.conditionOf(element);
        if (condition.isEmpty()) {
            return;
        }

        PsiFile psiFile = element.getContainingFile();
        VirtualFile file = psiFile == null ? null : psiFile.getOriginalFile().getVirtualFile();
        if (file == null) {
            return;
        }
        if (SandConditions.matches(condition, SandViewEnv.viewEnv(myProject, file))) {
            return;
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(SandHighlighterKeys.INACTIVE_BRANCH)
            .create();
    }

    private static boolean isDirective(IElementType type) {
        return type == SandElements.IF_DIRECTIVE
            || type == SandElements.IFNDEF_DIRECTIVE
            || type == SandElements.ELIF_DIRECTIVE
            || type == SandElements.ELSE_DIRECTIVE
            || type == SandElements.END_DIRECTIVE;
    }
}
