// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.language.uast;

import consulo.language.psi.PsiElement;
import consulo.language.uast.visitor.UastTypedVisitor;
import consulo.language.uast.visitor.UastVisitor;
import org.jspecify.annotations.Nullable;

/**
 * A {@link PsiElement} declaration wrapper.
 */
public interface UDeclaration extends UElement, UAnchorOwner {
    /**
     * Returns the declaration name, or null if the declaration is anonymous.
     */
    @Nullable String getName();

    /**
     * Returns the declaration name identifier. If declaration is anonymous other implementation dependant psi element will be returned.
     * The main rule that returned element is "anchor": it is a single token which represents this declaration.
     * <p>
     * It is useful for putting gutters and inspection reports.
     */
    @Override
    @Nullable UIdentifier getUastAnchor();

    /**
     * The element in the language declaration model that this declaration stands for - what resolve, find-usages and the language
     * declaration interfaces work with. Same as {@link #getSourcePsi()} for most languages; a binding may return a derived element
     * (for example a light class). Null for synthetic declarations.
     */
    default @Nullable PsiElement getDeclarationPsi() {
        return getSourcePsi();
    }

    @Override
    default void accept(UastVisitor visitor) {
        if (visitor.visitDeclaration(this)) {
            return;
        }
        visitor.afterVisitDeclaration(this);
    }

    @Override
    default <D, R> R accept(UastTypedVisitor<D, R> visitor, D data) {
        return visitor.visitDeclaration(this, data);
    }
}
