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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.*;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class InspectionManager {
  public static InspectionManager getInstance(Project project) {
    return project.getInstance(InspectionManager.class);
  }

  @Nonnull
  public abstract Project getProject();

  @Contract(pure = true)
  public abstract ModuleProblemDescriptor createProblemDescriptor(@Nonnull String descriptionTemplate, @Nonnull Module module, QuickFix<?> ... fixes);

  @Nonnull
  public abstract CommonProblemDescriptor createProblemDescriptor(@Nonnull String descriptionTemplate, QuickFix... fixes);

  /**
   * Factory method for ProblemDescriptor. Should be called from LocalInspectionTool.checkXXX() methods.
   *
   * @param psiElement          problem is reported against
   * @param descriptionTemplate problem message. Use <code>#ref</code> for a link to problem piece of code and <code>#loc</code> for location in source code.
   * @param fix                 should be null if no fix is provided.
   * @param onTheFly            for local tools on batch run
   */
  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                            @Nonnull String descriptionTemplate,
                                                            LocalQuickFix fix,
                                                            @Nonnull ProblemHighlightType highlightType,
                                                            boolean onTheFly);

  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                            @Nonnull String descriptionTemplate,
                                                            boolean onTheFly,
                                                            LocalQuickFix[] fixes,
                                                            @Nonnull ProblemHighlightType highlightType);

  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                            @Nonnull String descriptionTemplate,
                                                            LocalQuickFix[] fixes,
                                                            @Nonnull ProblemHighlightType highlightType,
                                                            boolean onTheFly,
                                                            boolean isAfterEndOfLine);

  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement startElement,
                                                            @Nonnull PsiElement endElement,
                                                            @Nonnull String descriptionTemplate,
                                                            @Nonnull ProblemHighlightType highlightType,
                                                            boolean onTheFly,
                                                            LocalQuickFix... fixes);

  /**
   * @param psiElement
   * @param rangeInElement      null means the text range of the element
   * @param descriptionTemplate
   * @param highlightType
   * @param onTheFly
   * @param fixes
   * @return
   */
  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                            @Nullable TextRange rangeInElement,
                                                            @Nonnull final String descriptionTemplate,
                                                            @Nonnull ProblemHighlightType highlightType,
                                                            boolean onTheFly,
                                                            LocalQuickFix... fixes);

  @Nonnull
  public abstract ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                            @Nonnull final String descriptionTemplate,
                                                            final boolean showTooltip,
                                                            @Nonnull ProblemHighlightType highlightType,
                                                            boolean onTheFly,
                                                            final LocalQuickFix... fixes);

  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, String, boolean, LocalQuickFix, ProblemHighlightType)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement, @Nonnull String descriptionTemplate, LocalQuickFix fix, @Nonnull ProblemHighlightType highlightType);

  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, String, boolean, LocalQuickFix[], ProblemHighlightType)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                                @Nonnull String descriptionTemplate,
                                                                LocalQuickFix[] fixes,
                                                                @Nonnull ProblemHighlightType highlightType);

  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, String, LocalQuickFix[], ProblemHighlightType, boolean, boolean)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                                @Nonnull String descriptionTemplate,
                                                                LocalQuickFix[] fixes,
                                                                @Nonnull ProblemHighlightType highlightType,
                                                                boolean isAfterEndOfLine);

  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, PsiElement, String, ProblemHighlightType, boolean, LocalQuickFix...)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement startElement,
                                                                @Nonnull PsiElement endElement,
                                                                @Nonnull String descriptionTemplate,
                                                                @Nonnull ProblemHighlightType highlightType,
                                                                LocalQuickFix... fixes);


  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, TextRange, String, ProblemHighlightType, boolean, LocalQuickFix...)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                                final TextRange rangeInElement,
                                                                @Nonnull final String descriptionTemplate,
                                                                @Nonnull ProblemHighlightType highlightType,
                                                                final LocalQuickFix... fixes);

  @Deprecated
  @Nonnull
  /**
   * use {@link #createProblemDescriptor(PsiElement, String, boolean, ProblemHighlightType, boolean, LocalQuickFix...)} instead
   */ public abstract ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                                @Nonnull final String descriptionTemplate,
                                                                final boolean showTooltip,
                                                                @Nonnull ProblemHighlightType highlightType,
                                                                final LocalQuickFix... fixes);

  @Nonnull
  public abstract GlobalInspectionContext createNewGlobalContext(boolean reuse);

  public abstract String getCurrentProfile();
}
