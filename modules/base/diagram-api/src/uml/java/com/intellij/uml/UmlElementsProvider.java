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

package com.intellij.uml;

import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

import java.util.Comparator;

/**
 * @author Konstantin Bulenkov
 */
public interface UmlElementsProvider<T> {
  Comparator<PsiElement> PSI_COMPARATOR = new Comparator<PsiElement>() {
    public int compare(PsiElement o1, PsiElement o2) {
      final boolean n1 = o1 instanceof PsiNamedElement;
      final boolean n2 = o2 instanceof PsiNamedElement;
      if (n1 && n2) {
        final String name1 = ((PsiNamedElement)o1).getName();
        final String name2 = ((PsiNamedElement)o2).getName();
        return name1 == null ? name2 == null ? 0 : 1 : name1.compareTo(name2);
      } else {
        return n1 ? -1 : 1;
      }
    }
  };

  UmlElementsProvider[] EMPTY_ARRAY = {};

  T[] getElements(T element, Project project);
  String getName();
  String getHeaderName(T element, Project project);
  ShortcutSet getShortcutSet();
  Comparator<? super T> getComparator();
  boolean showProgress();
  String getProgressMessage();
}
