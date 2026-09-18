// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.uast.visitor;

import consulo.language.uast.UDeclaration;
import consulo.language.uast.UElement;
import consulo.language.uast.UExpression;
import consulo.language.uast.UVariable;

/**
 * A visitor for UAST elements.
 * <p>
 * When an instance is passed to any {@link UElement}'s {@link UElement#accept(UastVisitor)} function, the appropriate {@code visit*}
 * function will be called, depending on the actual type of the element.
 * <p>
 * The default implementation for each {@code visit*} function other than {@link #visitElement} is to delegate to the {@code visit*}
 * function for the element's supertype. That lets you implement only the most general {@code visit*} method that applies to your
 * use case. For example, if you want to visit all variables, you can implement {@link #visitVariable} instead of the language-specific
 * variable hooks.
 * <p>
 * To visit the element's children as well, return {@code false} from the {@code visit*} function.
 * <p>
 * If the {@code visit*} function returns {@code false}, then the visitor will be passed to the {@code accept} function of each of the
 * direct children of the element, and then the visitor's {@code afterVisit*} will be called for the element's type. The default
 * implementation for each {@code afterVisit*} function other than {@link #afterVisitElement} is to delegate to the {@code afterVisit*}
 * function for the element's supertype.
 * <p>
 * The platform carries no node vocabulary: language modules extend {@code UastVisitor} with their own hooks (e.g. a
 * {@code JavaUastVisitor} with {@code visitClass}/{@code visitMethod}/{@code visitCallExpression}). Their elements dispatch via
 * {@code instanceof} on the visitor and fall back to the generic hook ({@link #visitExpression}, {@link #visitDeclaration},
 * {@link #visitElement}) for visitors that do not know them.
 */
public interface UastVisitor {
    boolean visitElement(UElement node);

    default boolean visitDeclaration(UDeclaration node) {
        return visitElement(node);
    }

    default boolean visitVariable(UVariable node) {
        return visitDeclaration(node);
    }

    // Expressions
    default boolean visitExpression(UExpression node) {
        return visitElement(node);
    }

    // After

    default void afterVisitElement(UElement node) {
    }

    default void afterVisitDeclaration(UDeclaration node) {
        afterVisitElement(node);
    }

    default void afterVisitVariable(UVariable node) {
        afterVisitElement(node);
    }

    // Expressions
    default void afterVisitExpression(UExpression node) {
        afterVisitElement(node);
    }
}
