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

import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class PsiDiagramNode<T extends PsiElement> extends DiagramNodeBase<T> {
    private final SmartPsiElementPointer<T> myPointer;
    private T myLastGoodPsiElement;

    public PsiDiagramNode(T psiElement, DiagramProvider<T> provider) {
        super(provider);
        myPointer = SmartPointerManager.getInstance(psiElement.getProject()).createSmartPsiElementPointer(psiElement);
        myLastGoodPsiElement = psiElement;
    }

    /**
     * Don't use myPointer.getElement() !!! Use getElement() instead
     *
     * @return PsiElement from smart pointer or link to last good PsiElement
     */
    protected final T getElement() {
        T element = myPointer.getElement();
        if (element != null) {
            myLastGoodPsiElement = element;
            return element;
        }
        return myLastGoodPsiElement;
    }

    @Override
    public @Nullable Image getIcon() {
        return IconDescriptorUpdaters.getIcon(getElement(), 0);
    }

    @Override
    public T getIdentifyingElement() {
        return getElement();
    }
}
