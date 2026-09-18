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
package consulo.language.uast.visitor;

import consulo.language.uast.UDeclaration;
import consulo.language.uast.UElement;
import consulo.language.uast.UExpression;
import consulo.language.uast.UVariable;

/**
 * A typed visitor for UAST elements: every {@code visit*} hook receives a data argument and produces a result.
 * <p>
 * Language modules extend this interface with their own hooks; their elements fall back to the generic hook
 * ({@link #visitExpression}, {@link #visitDeclaration}, {@link #visitElement}) for visitors that do not know them.
 */
public interface UastTypedVisitor<D, R> {
    R visitElement(UElement node, D data);

    // Declarations
    default R visitDeclaration(UDeclaration node, D data) {
        return visitElement(node, data);
    }

    // Variables
    default R visitVariable(UVariable node, D data) {
        return visitDeclaration(node, data);
    }

    // Expressions
    default R visitExpression(UExpression node, D data) {
        return visitElement(node, data);
    }
}
