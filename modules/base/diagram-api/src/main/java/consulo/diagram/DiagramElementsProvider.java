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
package consulo.diagram;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ShortcutSet;

import java.util.Comparator;

/**
 * @author Konstantin Bulenkov
 */
public interface DiagramElementsProvider<T> {
    Comparator<PsiElement> PSI_COMPARATOR = (o1, o2) -> {
        boolean n1 = o1 instanceof PsiNamedElement;
        boolean n2 = o2 instanceof PsiNamedElement;
        if (n1 && n2) {
            String name1 = ((PsiNamedElement) o1).getName();
            String name2 = ((PsiNamedElement) o2).getName();
            if (name1 == null) {
                return name2 == null ? 0 : 1;
            }
            return name2 == null ? -1 : name1.compareTo(name2);
        }
        return n1 ? -1 : 1;
    };

    @SuppressWarnings("rawtypes")
    DiagramElementsProvider[] EMPTY_ARRAY = {};

    T[] getElements(T element, Project project);

    LocalizeValue getName();

    String getHeaderName(T element, Project project);

    ShortcutSet getShortcutSet();

    Comparator<? super T> getComparator();

    boolean showProgress();

    LocalizeValue getProgressMessage();
}
