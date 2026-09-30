/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.diagram;

import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public abstract class PsiDiagramNode<T extends PsiElement> extends DiagramNodeBase<T> {
  private final SmartPsiElementPointer<T> myPointer;
  private T lastGoodPsiElement;

  public PsiDiagramNode(final T psiElement, @NotNull DiagramProvider<T> provider) {
    super(provider);
    myPointer = SmartPointerManager.getInstance(psiElement.getProject()).createSmartPsiElementPointer(psiElement);
    lastGoodPsiElement = psiElement;
  }

  /**
   * Don't use myPointer.getElement() !!! Use getElement() instead
   * @return PsiElement from smart pointer or link to last good PsiElement
   */
  protected final @NotNull T getElement() {
    final T element = myPointer.getElement();
    if (element != null) {
      lastGoodPsiElement = element;
      return element;
    } else {
      return lastGoodPsiElement;
    }
  }

  public Icon getIcon() {
    return getElement().getIcon(0);
  }

  @NotNull
  public T getIdentifyingElement() {
    return getElement();
  }
}
