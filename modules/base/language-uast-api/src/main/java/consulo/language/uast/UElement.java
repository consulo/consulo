// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.uast.visitor.UastTypedVisitor;
import consulo.language.uast.visitor.UastVisitor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The common interface for all Uast elements.
 */
public interface UElement {
    /**
     * Returns the element parent.
     */
    @Nullable UElement getUastParent();

    /**
     * Returns the PSI element underlying this element. Note that some UElements are synthetic and do not have
     * an underlying PSI element; this doesn't mean that they are invalid.
     * <p>
     * <b>Node for implementors</b>: please implement {@link #getSourcePsi()} or make it return {@code null} explicitly
     * if implementing is not possible. Redirect {@code psi} to it keeping existing behavior.
     *
     * @deprecated ambiguous psi element, use {@link #getSourcePsi()}
     */
    @Deprecated
    @Nullable PsiElement getPsi();

    /**
     * Returns the PSI element in original (physical) tree to which this UElement corresponds.
     * <b>Note</b>: that some UElements are synthetic and do not have an underlying PSI element;
     * this doesn't mean that they are invalid.
     */
    @SuppressWarnings("deprecation")
    default @Nullable PsiElement getSourcePsi() {
        return getPsi();
    }

    /**
     * Returns true if this element is valid, false otherwise.
     */
    default boolean isPsiValid() {
        PsiElement sourcePsi = getSourcePsi();
        return sourcePsi == null || sourcePsi.isValid();
    }

    /**
     * Returns the list of comments for this element.
     */
    default List<UComment> getComments() {
        return List.of();
    }

    /**
     * Returns the log string (usually one line containing the class name and some additional information).
     * <p>
     * Examples:
     * UWhileExpression
     * UBinaryExpression (>)
     * UCallExpression (println)
     * USimpleReferenceExpression (i)
     * ULiteralExpression (5)
     *
     * @return the expression tree for this element.
     */
    String asLogString();

    /**
     * Returns the string in pseudo-code.
     * <p>
     * Output example (should be something like this):
     * <pre>
     * while (i > 5) {
     *     println("Hello, world")
     *     i--
     * }
     * </pre>
     *
     * @return the rendered text.
     */
    default String asRenderString() {
        return asLogString();
    }

    /**
     * Returns the string as written in the source file.
     * Use this String only for logging and diagnostic text messages.
     *
     * @return the original text.
     */
    default String asSourceString() {
        return asRenderString();
    }

    /**
     * Passes the element to the specified visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    default void accept(UastVisitor visitor) {
        visitor.visitElement(this);
        visitor.afterVisitElement(this);
    }

    /**
     * Passes the element to the specified typed visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    default <D, R> R accept(UastTypedVisitor<D, R> visitor, D data) {
        return visitor.visitElement(this, data);
    }

    /**
     * NOTE: it is called {@code lang} instead of "language" to avoid clash with {@link PsiElement#getLanguage()} in classes which
     * implements both interfaces,
     *
     * @return language of the physical {@link PsiElement} this {@link UElement} was made from, or {@link Language#ANY} if no "physical"
     * language could be found
     */
    default Language getLang() {
        return UElementUtil.withContainingElements(this)
            .map(UElement::getSourcePsi)
            .filter(Objects::nonNull)
            .map(PsiElement::getLanguage)
            .findFirst()
            .orElse(Language.ANY);
    }
}
