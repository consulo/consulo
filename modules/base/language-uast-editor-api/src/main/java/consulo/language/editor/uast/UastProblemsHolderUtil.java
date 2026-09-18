// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.editor.uast;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.uast.UAnchorOwner;
import consulo.language.uast.UElement;
import consulo.language.uast.UIdentifier;
import consulo.localize.LocalizeValue;

/**
 * Registers problems on UAST elements: the problem is anchored to the source PSI of the element
 * (or of its {@link UAnchorOwner#getUastAnchor() anchor}) and silently skipped when there is nothing to anchor to.
 * <p>
 * Port of the generic {@code registerUProblem} extension functions of {@code com.intellij.codeInspection.problemHolderUtil};
 * the node-vocabulary specific overloads (call, reference) belong to the language modules, and the declaration overload
 * is covered by the {@link UAnchorOwner} one since {@code UDeclaration} extends {@link UAnchorOwner}.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
public final class UastProblemsHolderUtil {
    private UastProblemsHolderUtil() {
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UAnchorOwner element,
                                        LocalizeValue descriptionTemplate,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, descriptionTemplate, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UAnchorOwner element,
                                        LocalizeValue descriptionTemplate,
                                        ProblemHighlightType highlightType,
                                        LocalQuickFix... fixes) {
        UIdentifier uastAnchor = element.getUastAnchor();
        PsiElement anchor = uastAnchor == null ? null : uastAnchor.getSourcePsi();
        if (anchor == null) {
            return;
        }
        holder.newProblem(descriptionTemplate).range(anchor).highlightType(highlightType).withFixes(fixes).create();
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UAnchorOwner element,
                                        String descriptionTemplate,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, LocalizeValue.of(descriptionTemplate), fixes);
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UAnchorOwner element,
                                        String descriptionTemplate,
                                        ProblemHighlightType highlightType,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, LocalizeValue.of(descriptionTemplate), highlightType, fixes);
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UElement element,
                                        LocalizeValue descriptionTemplate,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, descriptionTemplate, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UElement element,
                                        LocalizeValue descriptionTemplate,
                                        ProblemHighlightType highlightType,
                                        LocalQuickFix... fixes) {
        PsiElement anchor = element.getSourcePsi();
        if (anchor == null) {
            return;
        }
        if (anchor.getTextLength() == 0) {
            return;
        }
        holder.newProblem(descriptionTemplate).range(anchor).highlightType(highlightType).withFixes(fixes).create();
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UElement element,
                                        String descriptionTemplate,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, LocalizeValue.of(descriptionTemplate), fixes);
    }

    @RequiredReadAction
    public static void registerUProblem(ProblemsHolder holder,
                                        UElement element,
                                        String descriptionTemplate,
                                        ProblemHighlightType highlightType,
                                        LocalQuickFix... fixes) {
        registerUProblem(holder, element, LocalizeValue.of(descriptionTemplate), highlightType, fixes);
    }
}
