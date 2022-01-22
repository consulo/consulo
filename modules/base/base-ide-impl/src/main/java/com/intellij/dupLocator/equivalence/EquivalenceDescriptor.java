package com.intellij.dupLocator.equivalence;

import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public interface EquivalenceDescriptor {
  List<PsiElement[]> getCodeBlocks();

  List<SingleChildDescriptor> getSingleChildDescriptors();

  List<MultiChildDescriptor> getMultiChildDescriptors();

  List<Object> getConstants();
}
