/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.testFramework;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

/**
 * @author peter
*/
public class MockProblemDescriptor extends ProblemDescriptorImpl {
  public MockProblemDescriptor(@Nonnull final PsiElement psiElement,
                               final String descriptionTemplate,
                               @Nonnull ProblemHighlightType highlightType,
                               final LocalQuickFix... fixes) {
    super(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, true);
  }

  @Override
  protected void assertPhysical(final PsiElement startElement) {
  }
}
