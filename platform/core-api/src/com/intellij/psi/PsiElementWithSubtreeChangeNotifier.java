package com.intellij.psi;

/**
 * @author VISTALL
 * @since 04.05.14
 */
public interface PsiElementWithSubtreeChangeNotifier extends PsiElement{
  void subtreeChanged();
}
