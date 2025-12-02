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
package consulo.language.editor.inspection.scheme;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class InspectionManager {
    public interface ProblemDescriptorBuilder {
        @Nonnull
        default ProblemDescriptorBuilder range(@Nonnull PsiElement element) {
            return range(element, element);
        }

        @Nonnull
        ProblemDescriptorBuilder range(@Nonnull PsiElement element, @Nullable TextRange rangeInElement);

        @Nonnull
        ProblemDescriptorBuilder range(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement);

        @Nonnull
        ProblemDescriptorBuilder highlightType(@Nonnull ProblemHighlightType highlightType);

        @Nonnull
        ProblemDescriptorBuilder afterEndOfLine();

        @Nonnull
        default ProblemDescriptorBuilder afterEndOfLine(boolean isAfterEndOfLine) {
            return isAfterEndOfLine ? afterEndOfLine() : this;
        }

        @Nonnull
        ProblemDescriptorBuilder onTheFly();

        @Nonnull
        default ProblemDescriptorBuilder onTheFly(boolean onTheFly) {
            return onTheFly ? onTheFly() : this;
        }

        @Nonnull
        default ProblemDescriptorBuilder hideTooltip() {
            return showTooltip(false);
        }

        @Nonnull
        ProblemDescriptorBuilder showTooltip(boolean showTooltip);

        @Nonnull
        ProblemDescriptorBuilder withFix(@Nonnull LocalQuickFix fix);

        default ProblemDescriptorBuilder withOptionalFix(@Nullable LocalQuickFix fix) {
            return fix != null ? withFix(fix) : this;
        }

        @Nonnull
        default ProblemDescriptorBuilder withFixes(LocalQuickFix... fixes) {
            if (fixes == null || fixes.length == 0) {
                return this;
            }
            return fixes.length == 1 ? withFix(fixes[0]) : withFixes(Arrays.asList(fixes));
        }

        @Nonnull
        ProblemDescriptorBuilder withFixes(@Nonnull Collection<? extends LocalQuickFix> localQuickFixes);

        @Nonnull
        @RequiredReadAction
        ProblemDescriptor create();
    }

    public static InspectionManager getInstance(Project project) {
        return project.getInstance(InspectionManager.class);
    }

    @Nonnull
    public abstract Project getProject();

    /**
     * Used in java plugin
     */
    @Nonnull
    @UsedInPlugin
    @RequiredReadAction
    public abstract List<ProblemDescriptor> runLocalToolLocaly(
        @Nonnull LocalInspectionTool tool,
        @Nonnull PsiFile file,
        @Nonnull Object state
    );

    @Nonnull
    @UsedInPlugin
    public abstract ProblemsHolder createProblemsHolder(@Nonnull PsiFile file, boolean onTheFly);

    @Contract(pure = true)
    public abstract ProblemDescriptorBuilder newProblemDescriptor(@Nonnull LocalizeValue descriptionTemplate);

    @Contract(pure = true)
    public abstract ModuleProblemDescriptor createProblemDescriptor(
        @Nonnull String descriptionTemplate,
        @Nonnull Module module,
        QuickFix<?>... fixes
    );

    @Nonnull
    public abstract CommonProblemDescriptor createProblemDescriptor(@Nonnull String descriptionTemplate, QuickFix... fixes);

    /**
     * Factory method for ProblemDescriptor. Should be called from LocalInspectionTool.checkXXX() methods.
     *
     * @param psiElement          problem is reported against
     * @param descriptionTemplate problem message. Use <code>#ref</code> for a link to problem piece of code and <code>#loc</code>
     *                            for location in source code.
     * @param fix                 should be null if no fix is provided.
     * @param onTheFly            for local tools on batch run
     */
    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix fix,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .onTheFly(onTheFly)
            .withOptionalFix(fix)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean onTheFly,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .onTheFly(onTheFly)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        boolean isAfterEndOfLine
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .afterEndOfLine(isAfterEndOfLine)
            .onTheFly(onTheFly)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(startElement, endElement)
            .highlightType(highlightType)
            .onTheFly(onTheFly)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nullable TextRange rangeInElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement, rangeInElement)
            .highlightType(highlightType)
            .onTheFly(onTheFly)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean showTooltip,
        @Nonnull ProblemHighlightType highlightType,
        boolean onTheFly,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .showTooltip(showTooltip)
            .onTheFly(onTheFly)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix fix,
        @Nonnull ProblemHighlightType highlightType
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .withOptionalFix(fix)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        LocalQuickFix[] fixes,
        @Nonnull ProblemHighlightType highlightType,
        boolean isAfterEndOfLine
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .afterEndOfLine(isAfterEndOfLine)
            .highlightType(highlightType)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(startElement, endElement)
            .highlightType(highlightType)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        TextRange rangeInElement,
        @Nonnull String descriptionTemplate,
        @Nonnull ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement, rangeInElement)
            .highlightType(highlightType)
            .withFixes(fixes)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use #newProblemDescriptor()...create()")
    @Nonnull
    @RequiredReadAction
    public ProblemDescriptor createProblemDescriptor(
        @Nonnull PsiElement psiElement,
        @Nonnull String descriptionTemplate,
        boolean showTooltip,
        @Nonnull ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        return newProblemDescriptor(LocalizeValue.of(descriptionTemplate))
            .range(psiElement)
            .highlightType(highlightType)
            .showTooltip(showTooltip)
            .withFixes(fixes)
            .create();
    }

    @Nonnull
    public abstract GlobalInspectionContext createNewGlobalContext(boolean reuse);

    public abstract String getCurrentProfile();
}
