package com.intellij.codeInspection;

import com.intellij.psi.PsiElement;

/**
 * This kind of suppression fix is able to provide suppression container.
 * Container might be used by IDEA for checking fix availability and highlighting suppression element.
 */
public interface ContainerBasedSuppressQuickFix extends SuppressQuickFix {
  @javax.annotation.Nullable
  PsiElement getContainer(@javax.annotation.Nullable PsiElement context);
}
