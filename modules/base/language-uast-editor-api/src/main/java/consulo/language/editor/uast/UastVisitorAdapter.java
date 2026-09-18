// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.uast;

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.language.uast.visitor.UastVisitor;

/**
 * A {@link PsiElementVisitor} which converts every visited PSI element to UAST (restricted to the given
 * UAST element types) and feeds the result to a non-recursive {@link UastVisitor}.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
public class UastVisitorAdapter extends PsiElementVisitor {
    private final UastLanguagePlugin myPlugin;
    private final UastVisitor myVisitor;
    private final boolean myDirectOnly;
    private final Class<? extends UElement>[] myUElementTypesHint;

    /**
     * @param plugin            the UAST plugin used to convert PSI elements
     * @param visitor           a non-recursive UAST visitor. (Note: visitor methods should return <b>true</b> to be non-recursive)
     * @param directOnly        if true only elements which are directly converted from passed {@link PsiElement} will be processed.
     *                          Setting to true is useful to avoid duplicating reports on {@code sourcePsi} elements.
     * @param uElementTypesHint the UAST element types the visitor is interested in
     */
    public UastVisitorAdapter(UastLanguagePlugin plugin,
                              UastVisitor visitor,
                              boolean directOnly,
                              Class<? extends UElement>[] uElementTypesHint) {
        myPlugin = plugin;
        myVisitor = visitor;
        myDirectOnly = directOnly;
        myUElementTypesHint = uElementTypesHint;
    }

    @Override
    public void visitElement(PsiElement element) {
        super.visitElement(element);
        UElement uElement = myPlugin.convertElementWithParent(element, myUElementTypesHint);
        if (uElement == null) {
            return;
        }
        if (myDirectOnly && uElement.getSourcePsi() != element) {
            return;
        }
        uElement.accept(myVisitor);
    }

    public static PsiElementVisitor create(Language language,
                                           UastVisitor visitor,
                                           Class<? extends UElement>[] uElementTypesHint) {
        return create(language, visitor, uElementTypesHint, true);
    }

    public static PsiElementVisitor create(Language language,
                                           UastVisitor visitor,
                                           Class<? extends UElement>[] uElementTypesHint,
                                           boolean directOnly) {
        UastLanguagePlugin plugin = UastLanguagePlugin.byLanguage(language);
        if (plugin == null) {
            return EMPTY_VISITOR;
        }
        if (uElementTypesHint.length == 1) {
            return new SimpleUastHintedVisitorAdapter(plugin, visitor, uElementTypesHint[0], directOnly);
        }

        return new UastVisitorAdapter(plugin, visitor, directOnly, uElementTypesHint);
    }

    /**
     * Fast path for a single hinted type: converts with {@link UastLanguagePlugin#convertElementWithParent(PsiElement, Class)}
     * instead of the array overload.
     */
    private static final class SimpleUastHintedVisitorAdapter extends PsiElementVisitor {
        private final UastLanguagePlugin myPlugin;
        private final UastVisitor myVisitor;
        private final Class<? extends UElement> myUElementTypesHint;
        private final boolean myDirectOnly;

        private SimpleUastHintedVisitorAdapter(UastLanguagePlugin plugin,
                                               UastVisitor visitor,
                                               Class<? extends UElement> uElementTypesHint,
                                               boolean directOnly) {
            myPlugin = plugin;
            myVisitor = visitor;
            myUElementTypesHint = uElementTypesHint;
            myDirectOnly = directOnly;
        }

        @Override
        public void visitElement(PsiElement element) {
            UElement uElement = myPlugin.convertElementWithParent(element, myUElementTypesHint);
            if (uElement == null) {
                return;
            }
            if (!myDirectOnly || uElement.getSourcePsi() == element) {
                uElement.accept(myVisitor);
            }
        }
    }
}
