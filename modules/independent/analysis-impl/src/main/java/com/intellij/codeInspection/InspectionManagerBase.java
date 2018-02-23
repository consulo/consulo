/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

public abstract class InspectionManagerBase extends InspectionManager {
  private final Project myProject;
  @NonNls protected String myCurrentProfileName;

  public InspectionManagerBase(Project project) {
    myProject = project;
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public CommonProblemDescriptor createProblemDescriptor(@Nonnull String descriptionTemplate, QuickFix... fixes) {
    return new CommonProblemDescriptorImpl(fixes, descriptionTemplate);
  }

  @Override
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   LocalQuickFix fix,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   boolean onTheFly) {
    LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
    return createProblemDescriptor(psiElement, descriptionTemplate, onTheFly, quickFixes, highlightType);
  }

  @Override
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   boolean onTheFly,
                                                   LocalQuickFix[] fixes,
                                                   @Nonnull ProblemHighlightType highlightType) {
    return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, onTheFly, false);
  }

  @Override
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   LocalQuickFix[] fixes,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   boolean onTheFly,
                                                   boolean isAfterEndOfLine) {
    return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, null, true, onTheFly);
  }

  @Override
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement startElement,
                                                   @Nonnull PsiElement endElement,
                                                   @Nonnull String descriptionTemplate,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   boolean onTheFly,
                                                   LocalQuickFix... fixes) {
    return new ProblemDescriptorBase(startElement, endElement, descriptionTemplate, fixes, highlightType, false, null, true, onTheFly);
  }

  @Nonnull
  @Override
  public ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                   final TextRange rangeInElement,
                                                   @Nonnull final String descriptionTemplate,
                                                   @Nonnull final ProblemHighlightType highlightType,
                                                   boolean onTheFly,
                                                   final LocalQuickFix... fixes) {
    return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, rangeInElement, true, onTheFly);
  }

  @Nonnull
  @Override
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   boolean showTooltip,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   boolean onTheFly,
                                                   LocalQuickFix... fixes) {
    return new ProblemDescriptorBase(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, showTooltip, onTheFly);
  }

  @Override
  @Deprecated
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   LocalQuickFix fix,
                                                   @Nonnull ProblemHighlightType highlightType) {
    LocalQuickFix[] quickFixes = fix != null ? new LocalQuickFix[]{fix} : null;
    return createProblemDescriptor(psiElement, descriptionTemplate, false, quickFixes, highlightType);
  }

  @Override
  @Deprecated
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   LocalQuickFix[] fixes,
                                                   @Nonnull ProblemHighlightType highlightType) {
    return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, false, false);
  }

  @Override
  @Deprecated
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   LocalQuickFix[] fixes,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   boolean isAfterEndOfLine) {
    return createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, true, isAfterEndOfLine);
  }

  @Override
  @Deprecated
  @Nonnull
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement startElement,
                                                   @Nonnull PsiElement endElement,
                                                   @Nonnull String descriptionTemplate,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   LocalQuickFix... fixes) {
    return createProblemDescriptor(startElement, endElement, descriptionTemplate, highlightType, true, fixes);
  }

  @Nonnull
  @Override
  @Deprecated
  public ProblemDescriptor createProblemDescriptor(@Nonnull final PsiElement psiElement,
                                                   final TextRange rangeInElement,
                                                   @Nonnull final String descriptionTemplate,
                                                   @Nonnull final ProblemHighlightType highlightType,
                                                   final LocalQuickFix... fixes) {
    return createProblemDescriptor(psiElement, rangeInElement, descriptionTemplate, highlightType, true, fixes);
  }

  @Nonnull
  @Deprecated
  @Override
  public ProblemDescriptor createProblemDescriptor(@Nonnull PsiElement psiElement,
                                                   @Nonnull String descriptionTemplate,
                                                   boolean showTooltip,
                                                   @Nonnull ProblemHighlightType highlightType,
                                                   LocalQuickFix... fixes) {
    return createProblemDescriptor(psiElement, descriptionTemplate, showTooltip, highlightType, true, fixes);
  }

  public String getCurrentProfile() {
    if (myCurrentProfileName == null) {
      final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(getProject());
      myCurrentProfileName = profileManager.getProjectProfile();
      if (myCurrentProfileName == null) {
        myCurrentProfileName = InspectionProfileManager.getInstance().getRootProfile().getName();
      }
    }
    return myCurrentProfileName;
  }
}
