// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.language.editor.ui.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.navigation.TargetPresentation;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One row of a target popup. Holds a pointer rather than the element itself, so an open popup never
 * keeps PSI alive, and the presentation it was built with.
 */
public final class ItemWithPresentation<T extends PsiElement> {
    private final SmartPsiElementPointer<T> myPointer;
    private final TargetPresentation myPresentation;

    public ItemWithPresentation(SmartPsiElementPointer<T> pointer, TargetPresentation presentation) {
        myPointer = pointer;
        myPresentation = presentation;
    }

    /**
     * For rows built where the element is already in hand under a read lock - a search callback feeding
     * an open popup, most of all.
     */
    @RequiredReadAction
    public ItemWithPresentation(T element, TargetPresentationProvider<? super T> provider) {
        this(SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element), provider.getPresentation(element));
    }

    public SmartPsiElementPointer<T> getPointer() {
        return myPointer;
    }

    public TargetPresentation getPresentation() {
        return myPresentation;
    }

    public @Nullable T dereference() {
        return myPointer.getElement();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemWithPresentation<?> item)) {
            return false;
        }
        return myPointer.equals(item.myPointer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(myPointer);
    }
}
