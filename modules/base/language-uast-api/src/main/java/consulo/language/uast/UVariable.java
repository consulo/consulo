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
package consulo.language.uast;

import consulo.language.uast.util.UastImplementationUtil;
import consulo.language.uast.visitor.UastTypedVisitor;
import consulo.language.uast.visitor.UastVisitor;
import org.jspecify.annotations.Nullable;

/**
 * A variable wrapper to be used in {@link UastVisitor}.
 */
public interface UVariable extends UDeclaration {
    /**
     * Returns the variable initializer or the parameter default value, or null if the variable has not an initializer.
     */
    @Nullable UExpression getUastInitializer();

    @Override
    default void accept(UastVisitor visitor) {
        if (visitor.visitVariable(this)) {
            return;
        }
        visitContents(visitor);
        visitor.afterVisitVariable(this);
    }

    @Override
    default <D, R> R accept(UastTypedVisitor<D, R> visitor, D data) {
        return visitor.visitVariable(this, data);
    }

    @Override
    default String asLogString() {
        return UastImplementationUtil.log(this, "name = " + getName());
    }

    @Override
    default String asRenderString() {
        StringBuilder builder = new StringBuilder();
        builder.append("var ").append(getName());
        UExpression initializer = getUastInitializer();
        if (initializer != null) {
            builder.append(" = ").append(initializer.asRenderString());
        }
        return builder.toString();
    }

    private void visitContents(UastVisitor visitor) {
        UExpression initializer = getUastInitializer();
        if (initializer != null) {
            initializer.accept(visitor);
        }
    }
}
