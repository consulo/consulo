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

import consulo.language.uast.visitor.UastTypedVisitor;
import consulo.language.uast.visitor.UastVisitor;
import org.jspecify.annotations.Nullable;

/**
 * Represents an expression or statement (which is considered as an expression in Uast).
 */
public interface UExpression extends UElement {
    /**
     * Returns the expression value or null if the value can't be calculated.
     */
    default @Nullable Object evaluate() {
        return null;
    }

    /**
     * Returns {@code true} if this node yields a value, {@code false} for statement-like nodes;
     * language implementations override this for statements.
     */
    default boolean isValueProducing() {
        return true;
    }

    @Override
    default void accept(UastVisitor visitor) {
        if (visitor.visitExpression(this)) {
            return;
        }
        visitor.afterVisitExpression(this);
    }

    @Override
    default <D, R> R accept(UastTypedVisitor<D, R> visitor, D data) {
        return visitor.visitExpression(this, data);
    }
}
